package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormAttachment;
import legal.shrinkwrap.api.persistence.entity.NormAbbreviationEntity;
import legal.shrinkwrap.api.persistence.entity.NormDocumentEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import legal.shrinkwrap.api.persistence.entity.NormSectionType;
import legal.shrinkwrap.api.persistence.repo.NormAbbreviationRepository;
import legal.shrinkwrap.api.persistence.repo.NormDocumentRepository;
import legal.shrinkwrap.api.persistence.repo.NormRepository;
import legal.shrinkwrap.api.utils.PandocTextWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reading side of the norm mirror: resolve a law by the name a caller used, list the provisions
 * in force on a given day, and fetch the text of one of them on demand.
 */
@Service
@AllArgsConstructor
@Slf4j
public class NormService {

    /**
     * Raised when the html preprocessing changes, so rows converted by an older version can be
     * converted again on purpose rather than being trusted forever.
     */
    public static final int TEXT_CONVERSION_VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Bound on the title fallback - a query like "gesetz" would otherwise match thousands. */
    private static final int TITLE_MATCH_LIMIT = 50;

    /**
     * The RIS user interface. Without the "www", which resolves just as well and without a
     * redirect - measured across federal, state, annex and JGS links - and saves four characters
     * on every provision an agent reads.
     * <p>
     * A constant rather than a setting, unlike the document base of {@link NormTextService}:
     * that one is written into stored html, where a wrong host would be baked into the data,
     * while this link is built per response and changes with a redeploy.
     */
    private static final String RIS_UI = "https://ris.bka.gv.at";

    private final NormAbbreviationRepository normAbbreviationRepository;
    private final NormDocumentRepository normDocumentRepository;
    private final NormRepository normRepository;
    private final NormTextService normTextService;
    private final HtmlDownloadService htmlDownloadService;

    /**
     * Resolves whatever the caller wrote - an abbreviation or a short title, with or without the
     * year - to the laws it can mean. A list, because the same name can belong to several laws;
     * handing that ambiguity back is the correct answer, picking one silently is not.
     * <p>
     * The jurisdiction is required rather than guessed: "BauO" exists in all nine states.
     */
    public List<NormEntity> findLaws(String query, String jurisdiction) {
        Set<String> keys = NormAbbreviations.normalize(query);
        if (keys.isEmpty()) {
            return List.of();
        }
        List<Long> ids = normAbbreviationRepository.findNormIdsByNormalizedIn(keys, jurisdiction);
        if (!ids.isEmpty()) {
            return normRepository.findAllById(ids);
        }
        //nothing under that exact name - fall back to a substring match on the titles, so that
        //"Wohnrechtsänderungsgesetz" still reaches the "2. Wohnrechtsänderungsgesetz".
        //Shortest title first: the closer a title is in length, the less it carries besides the query
        return normRepository.findByTitleLike(jurisdiction,
                "%" + query.trim().toLowerCase(Locale.ROOT) + "%", Limit.of(TITLE_MATCH_LIMIT));
    }

    public Optional<NormEntity> findLaw(String jurisdiction, String gesetzesnummer) {
        return normRepository.findByJurisdictionAndIdentifier(jurisdiction, gesetzesnummer);
    }

    /** Every name the law is known by, for display - the normalised keys stay internal. */
    public List<String> namesOf(NormEntity norm) {
        return normAbbreviationRepository.findByNorm(norm).stream()
                .map(NormAbbreviationEntity::getAbkuerzung)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Link to the provision in the RIS user interface, for a caller that wants to point a reader
     * at the source. Stored is only the ELI path without its document segment, because that
     * segment is what makes it a single version rather than the provision across versions - and
     * because RIS hands out the ELI with a changing host, sometimes {@code ris.bka.gv.at} and
     * sometimes {@code www.ris.bka.gv.at}, within one and the same response. Putting the
     * document number back on the end yields the address the interface resolves.
     *
     * @return null when RIS supplied no ELI for the document
     */
    public String risUrl(NormDocumentEntity document) {
        if (document.getEliProvision() == null || document.getDocNumber() == null) {
            return null;
        }
        return RIS_UI + document.getEliProvision() + "/" + document.getDocNumber();
    }

    public Optional<NormDocumentEntity> findByDocNumber(String docNumber) {
        return normDocumentRepository.findByDocNumber(docNumber);
    }

    /** The provisions of a law in force on a day, in the order RIS lists them. */
    public List<NormDocumentEntity> findProvisions(NormEntity norm, LocalDate asOf) {
        return normDocumentRepository.findInForce(norm, asOf);
    }

    /**
     * The provisions between two designations, inclusive. "§§ 12a bis 14c" has to bring up 13,
     * 13a and 13aa as well, so the bounds are compared as (number, letters) rather than as text -
     * "13a" is not between "12a" and "14c" by string comparison.
     * <p>
     * The comparison runs in memory on the provisions of one law, which is a few hundred rows.
     * Expressing it in SQL would mean parsing a string column in the query for no gain.
     * <p>
     * Without a type the range spans every kind of provision, and a law can number several kinds
     * alike: the 2. Wohnrechtsänderungsgesetz has Art. 1 to Art. 5 <em>and</em> Art. 4 § 1 to
     * § 4, so "1 to 6" legitimately matches both. Either pass {@code typ}, or write the bound
     * as "§ 1" and let the designation say which kind is meant.
     *
     * @param typ restricts to one kind of provision; null takes it from the designation in
     *            {@code von}, and if that carries none, spans all kinds
     * @throws IllegalArgumentException if a bound cannot be read - an unusable range is an error,
     *                                  not an empty result
     */
    public List<NormDocumentEntity> findProvisions(NormEntity norm, LocalDate asOf,
                                                   String von, String bis, NormSectionType typ) {
        ArticleNumber from = ArticleNumber.parse(von);
        ArticleNumber to = ArticleNumber.parse(bis);
        if (from == null || to == null) {
            throw new IllegalArgumentException("cannot read the range " + von + " to " + bis
                    + " - expected a number, optionally with a designation: 12, 12a, \u00a7 12a, "
                    + "Art. 4, Anlage 3");
        }
        NormSectionType wanted = typ != null ? typ : ArticleNumber.sectionTypeOf(von);

        //no re-sorting: the order comes from RIS and is the reading order of the law. Sorting by
        //(number, letters) would interleave kinds that number alike - "Art. 1, Art. 4 § 1,
        //Art. 2, Art. 4 § 2, …" instead of the articles followed by their paragraphs
        return findProvisions(norm, asOf).stream()
                .filter(d -> wanted == null || wanted.name().equals(d.getAbschnittTyp()))
                .filter(d -> {
                    ArticleNumber key = ArticleNumber.of(d);
                    return key != null && key.compareTo(from) >= 0 && key.compareTo(to) <= 0;
                })
                .toList();
    }

    /**
     * Returns the provision with its text, fetching and converting it if that has not happened
     * yet. This is where the mirror stops being metadata only.
     */
    @Transactional
    public NormDocumentEntity withText(NormDocumentEntity document) {
        if (document.getFullText() != null
                && Objects.equals(document.getTextConversionVersion(), TEXT_CONVERSION_VERSION)) {
            return document;
        }
        if (document.getHtmlUrl() == null) {
            return document;
        }

        String html = htmlDownloadService.downloadHtml(document.getHtmlUrl());
        String cleanHtml = normTextService.prepareRisNormHtml(html, attachmentsOf(document));
        String text = PandocTextWrapper.convertHtmlToText(cleanHtml);
        if (text == null) {
            //pandoc failed; leave the row untouched so the next run tries again
            log.error("text conversion failed for {}", document.getDocNumber());
            return document;
        }

        document.setFullCleanHtml(cleanHtml);
        document.setFullText(text);
        document.setWordCount((long) text.split("\\s+").length);
        document.setTextConversionVersion(TEXT_CONVERSION_VERSION);
        return normDocumentRepository.save(document);
    }

    /**
     * The attachments as stored at import time. A plain attachment is mentioned nowhere in the
     * html, so without them the conversion cannot put a link to it into the text.
     */
    private List<RisNormAttachment> attachmentsOf(NormDocumentEntity document) {
        if (document.getAttachments() == null || document.getAttachments().isBlank()) {
            return List.of();
        }
        try {
            return List.of(MAPPER.readValue(document.getAttachments(), RisNormAttachment[].class));
        } catch (JsonProcessingException e) {
            log.error("could not read attachments of {}: {}", document.getDocNumber(), e.getMessage());
            return List.of();
        }
    }

    /**
     * A provision designation split into its number and its letters, so that a range can be
     * compared the way a lawyer reads it.
     */
    record ArticleNumber(int number, String letters) implements Comparable<ArticleNumber> {

        /**
         * Designations a caller may put in front of the number, and what they mean. Abbreviated
         * and written out, with or without the dot - "Art 1", "Art. 1" and "Artikel 1a" are the
         * same thing. "Anhang" counts as an Anlage: it is the word EUR-Lex uses, and callers
         * reach for it either way.
         */
        private static final Map<String, NormSectionType> PREFIXES = Map.of(
                "§", NormSectionType.PARAGRAPH,
                "§§", NormSectionType.PARAGRAPH,
                "paragraph", NormSectionType.PARAGRAPH,
                "art", NormSectionType.ARTIKEL,
                "artikel", NormSectionType.ARTIKEL,
                "anl", NormSectionType.ANLAGE,
                "anlage", NormSectionType.ANLAGE,
                "anh", NormSectionType.ANLAGE,
                "anhang", NormSectionType.ANLAGE);

        /**
         * Reads "12", "12a", "§ 12a", "§12a", "Paragraph 12", "Art 4", "Art. 4", "Artikel 4a", "Anl 3",
         * "Anl. 3", "Anlage 3", "Anh 3", "Anh. 3" or "Anhang 3". The designation is optional;
         * what has to be there is a number.
         */
        static ArticleNumber parse(String designation) {
            String value = withoutPrefix(designation);
            if (value == null) {
                return null;
            }
            int split = 0;
            while (split < value.length() && Character.isDigit(value.charAt(split))) {
                split++;
            }
            if (split == 0) {
                return null;
            }
            return new ArticleNumber(Integer.parseInt(value.substring(0, split)), value.substring(split));
        }

        /** The kind a designation names, or null when it carries none. */
        static NormSectionType sectionTypeOf(String designation) {
            if (designation == null) {
                return null;
            }
            String value = designation.trim().toLowerCase(Locale.ROOT);
            return PREFIXES.entrySet().stream()
                    .filter(e -> value.startsWith(e.getKey()))
                    .max(Comparator.comparingInt(e -> e.getKey().length()))
                    .map(Map.Entry::getValue)
                    .orElse(null);
        }

        /** Strips a leading designation together with any dot and spaces after it. */
        private static String withoutPrefix(String designation) {
            if (designation == null || designation.isBlank()) {
                return null;
            }
            String value = designation.trim().toLowerCase(Locale.ROOT);
            for (String prefix : PREFIXES.keySet()) {
                if (value.startsWith(prefix) && value.length() > prefix.length()) {
                    String rest = value.substring(prefix.length()).stripLeading();
                    rest = rest.startsWith(".") ? rest.substring(1).stripLeading() : rest;
                    //"artikel" also starts with "art", so only a prefix that leaves a number counts
                    if (!rest.isEmpty() && Character.isDigit(rest.charAt(0))) {
                        return rest;
                    }
                }
            }
            return value;
        }

        static ArticleNumber of(NormDocumentEntity document) {
            ArticleNumber base = parse(document.getNummer());
            if (base == null) {
                return null;
            }
            String letters = document.getBuchstabe() == null ? "" : document.getBuchstabe().toLowerCase();
            return new ArticleNumber(base.number(), base.letters() + letters);
        }

        /**
         * Numerically first, then by the letters, with no letters sorting before any. That is
         * how the provisions themselves run: § 1 comes before § 1a, § 1a before § 1b, and all of
         * them before § 2. A range from "1" therefore takes in 1a and 1b, while a range from
         * "1b" leaves 1a behind and still reaches 1c and 1d.
         */
        @Override
        public int compareTo(ArticleNumber other) {
            int byNumber = Integer.compare(number, other.number);
            return byNumber != 0 ? byNumber : letters.compareTo(other.letters);
        }
    }
}
