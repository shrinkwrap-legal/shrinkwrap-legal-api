package legal.shrinkwrap.api.controller;

import legal.shrinkwrap.api.dto.NormAmbiguityDto;
import legal.shrinkwrap.api.dto.NormLawDto;
import legal.shrinkwrap.api.dto.NormProvisionDto;
import legal.shrinkwrap.api.dto.NormStructureDto;
import legal.shrinkwrap.api.persistence.entity.NormDocumentEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import legal.shrinkwrap.api.persistence.entity.NormSectionType;
import legal.shrinkwrap.api.service.NormService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Consolidated law. The path prefix "api" is added by {@code WebMvcConfiguration}.
 * <p>
 * Jurisdiction is required everywhere and never inferred - "BauO" exists in all nine states, so
 * resolving it by guesswork would be wrong more often than right.
 */
@RestController
@Validated
@AllArgsConstructor
@Slf4j
public class NormController {

    /** Enough for the whole of a large law - the ABGB has 2.566 documents - and a bound against
     * an unfiltered request. */
    private static final int MAX_LIMIT = 2000;

    /** "Kompetenzfeststellung durch den VfGH" names 71 laws; a caller needs a choice, not all of them. */
    private static final int MAX_CANDIDATES = 25;

    private final NormService normService;

    /** Finds a law by an abbreviation or a short title, however the caller writes it. */
    @GetMapping(value = "norm/laws", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<NormLawDto> findLaws(@RequestParam("jurisdiction") String jurisdiction,
                                     @RequestParam("query") String query,
                                     @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return normService.findLaws(query, jurisdiction).stream()
                .limit(Math.min(limit, MAX_LIMIT))
                .map(this::toLawDto)
                .toList();
    }

    /**
     * The table of contents of a law without any text, so a caller can see what exists - and,
     * through hasText and wordCount, what retrieving it would cost.
     */
    @GetMapping(value = "norm/structure", produces = MediaType.APPLICATION_JSON_VALUE)
    public NormStructureDto getStructure(@RequestParam("jurisdiction") String jurisdiction,
                                         @RequestParam("gesetzesnummer") String gesetzesnummer,
                                         @RequestParam(value = "asOf", required = false) LocalDate asOf,
                                         @RequestParam(value = "inForceFrom", required = false) LocalDate inForceFrom,
                                         @RequestParam(value = "inForceUntil", required = false) LocalDate inForceUntil,
                                         @RequestParam(value = "from", required = false) String from,
                                         @RequestParam(value = "to", required = false) String to,
                                         @RequestParam(value = "typ", required = false) String typ) {
        NormEntity norm = law(jurisdiction, gesetzesnummer);
        return new NormStructureDto(toLawDto(norm),
                provisions(norm, asOf, inForceFrom, inForceUntil, from, to, typ).stream()
                        .map(d -> toProvisionDto(normService.withText(d), false)).toList());
    }

    /**
     * Provisions of a law, optionally with their text. A single provision is {@code from}
     * without {@code to}; a range includes everything in between, so 12a to 14c also brings up
     * 13 and 13a.
     * <p>
     * By default this answers with the law as it stands today. {@code asOf} moves that to another
     * day; {@code inForceFrom} and {@code inForceUntil} open it into a stretch of time and then
     * every version that was in force at any point within it comes back - which is how a caller
     * sees what changed. Either end may be left off. The versions of one provision arrive next
     * to each other, oldest first, each naming the publication that brought it about.
     * <p>
     * A bound is a number, optionally preceded by its designation: {@code 12}, {@code 12a},
     * {@code § 12a}, {@code Art. 4}, {@code Artikel 4}, {@code Anl. 3}, {@code Anlage 3}. The
     * designation also says which kind of provision is meant, so {@code from=§ 1} brings up
     * paragraphs only. Failing that, {@code typ} does the same, and without either the range
     * spans every kind - which matters, because a law can number several kinds alike: the
     * 2. Wohnrechtsänderungsgesetz has Art. 1 to Art. 5 as well as Art. 4 § 1 to § 4.
     */
    @GetMapping(value = "norm/provisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<NormProvisionDto> getProvisions(
            @RequestParam("jurisdiction") String jurisdiction,
            @RequestParam(value = "gesetzesnummer", required = false) String gesetzesnummer,
            @RequestParam(value = "abbreviation", required = false) String abbreviation,
            @RequestParam(value = "asOf", required = false) LocalDate asOf,
            @RequestParam(value = "inForceFrom", required = false) LocalDate inForceFrom,
            @RequestParam(value = "inForceUntil", required = false) LocalDate inForceUntil,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "typ", required = false) String typ,
            @RequestParam(value = "includeText", defaultValue = "false") boolean includeText,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        NormEntity norm = gesetzesnummer != null
                ? law(jurisdiction, gesetzesnummer)
                : singleLawFor(jurisdiction, abbreviation);

        return provisions(norm, asOf, inForceFrom, inForceUntil, from, to, typ).stream()
                .limit(Math.min(limit, MAX_LIMIT))
                //always converted, not only when the text is wanted: the word count comes out of
                //the conversion and there is nothing in the RIS metadata to derive it from. The
                //result is stored, so this costs one fetch per provision ever, not per request
                .map(d -> toProvisionDto(normService.withText(d), includeText))
                .toList();
    }

    /** The stable single reference; a document number never changes. */
    @GetMapping(value = "norm/document/{docNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public NormProvisionDto getDocument(@PathVariable("docNumber") String docNumber,
                                        @RequestParam(value = "includeText", defaultValue = "true") boolean includeText) {
        NormDocumentEntity document = normService.findByDocNumber(docNumber)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "unknown document " + docNumber));
        return toProvisionDto(includeText ? normService.withText(document) : document, includeText);
    }

    private List<NormDocumentEntity> provisions(NormEntity norm, LocalDate asOf,
                                                LocalDate inForceFrom, LocalDate inForceUntil,
                                                String from, String to, String typ) {
        LocalDate[] period = period(asOf, inForceFrom, inForceUntil);
        NormSectionType wanted = sectionType(typ);
        if (from == null) {
            return withoutLawHead(normService.findProvisions(norm, period[0], period[1])).stream()
                    .filter(d -> wanted == null || wanted.name().equals(d.getAbschnittTyp()))
                    .toList();
        }
        try {
            //a single provision is a range whose bounds coincide
            return withoutLawHead(normService.findProvisions(norm, period[0], period[1],
                    from, to == null ? from : to, wanted));
        } catch (IllegalArgumentException e) {
            //a bound nobody can read is a bad request, not an empty law
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Which stretch of time a provision has to have been in force in. A period wins over
     * {@code asOf}, either of its ends may be left open, and with neither given the answer is
     * today's law - the question a caller asks unless they say otherwise.
     */
    private static LocalDate[] period(LocalDate asOf, LocalDate inForceFrom, LocalDate inForceUntil) {
        if (inForceFrom != null || inForceUntil != null) {
            if (inForceFrom != null && inForceUntil != null && inForceFrom.isAfter(inForceUntil)) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "inForceFrom " + inForceFrom + " lies after inForceUntil " + inForceUntil);
            }
            return new LocalDate[]{inForceFrom, inForceUntil};
        }
        LocalDate day = asOf == null ? LocalDate.now() : asOf;
        return new LocalDate[]{day, day};
    }

    /** Accepts the enum in any casing, plus "Alle" for no restriction. */
    private NormSectionType sectionType(String typ) {
        if (typ == null || typ.isBlank() || "Alle".equalsIgnoreCase(typ.trim())) {
            return null;
        }
        try {
            return NormSectionType.valueOf(typ.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "unknown typ " + typ
                    + " - expected Paragraph, Artikel, Anlage, Norm or Alle");
        }
    }

    /**
     * Drops the head document of the law. RIS numbers it "§ 0" and marks it {@code Norm}; its
     * text block is empty, and what it carries besides - title, index, publication - is already
     * on the law itself. All 25.213 of them in the corpus are exactly the documents typed
     * {@code NORM}, so the two ways of naming them agree.
     */
    private static List<NormDocumentEntity> withoutLawHead(List<NormDocumentEntity> documents) {
        return documents.stream()
                .filter(d -> !NormSectionType.NORM.name().equals(d.getAbschnittTyp()))
                .toList();
    }

    private NormEntity law(String jurisdiction, String gesetzesnummer) {
        return normService.findLaw(jurisdiction, gesetzesnummer)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                        "unknown law " + gesetzesnummer + " in " + jurisdiction));
    }

    /**
     * An abbreviation can belong to several laws. Rather than picking one, the caller is told to
     * disambiguate - which of the two "UStG 1994" is meant is not something to guess at.
     */
    private NormEntity singleLawFor(String jurisdiction, String abbreviation) {
        if (abbreviation == null) {
            throw new ResponseStatusException(NOT_FOUND, "either gesetzesnummer or abbreviation is required");
        }
        List<NormEntity> laws = normService.findLaws(abbreviation, jurisdiction);
        if (laws.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "no law found for " + abbreviation);
        }
        if (laws.size() > 1) {
            throw new AmbiguousName(abbreviation, laws);
        }
        return laws.getFirst();
    }

    /**
     * A name that means several laws is not an error the caller can do anything with unless they
     * are told which laws. The candidates come back as data, newest first, so an agent can pick
     * one and repeat the request with its gesetzesnummer.
     */
    @ExceptionHandler(AmbiguousName.class)
    public ResponseEntity<NormAmbiguityDto> onAmbiguousName(AmbiguousName e) {
        List<NormAmbiguityDto.Candidate> candidates = e.laws.stream()
                .sorted(Comparator.comparing(NormEntity::getTitleSourceInkrafttreten,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_CANDIDATES)
                .map(n -> new NormAmbiguityDto.Candidate(n.getJurisdiction(), n.getIdentifier(),
                        n.getKurztitel(), n.getTyp(), n.getTitleSourceInkrafttreten()))
                .toList();
        return ResponseEntity.status(CONFLICT)
                .body(new NormAmbiguityDto(e.name, e.laws.size(), candidates));
    }

    /** Carries the candidates to {@link #onAmbiguousName}; never leaves this controller. */
    private static class AmbiguousName extends RuntimeException {
        private final String name;
        private final transient List<NormEntity> laws;

        AmbiguousName(String name, List<NormEntity> laws) {
            super(name);
            this.name = name;
            this.laws = laws;
        }
    }

    private NormLawDto toLawDto(NormEntity norm) {
        return new NormLawDto(norm.getJurisdiction(), norm.getIdentifier(), norm.getKurztitel(),
                norm.getLangtitel(), normService.namesOf(norm), norm.getTyp(),
                norm.getKundmachungsorgan(), norm.getEli());
    }

    /** "BGBl. I Nr." and "59/2017" are two RIS fields; a reader wants the one line. */
    private static String publication(String organ, String nummer) {
        if (organ == null && nummer == null) {
            return null;
        }
        return ((organ == null ? "" : organ) + " " + (nummer == null ? "" : nummer)).trim();
    }

    private NormProvisionDto toProvisionDto(NormDocumentEntity document, boolean includeText) {
        return new NormProvisionDto(
                document.getArtikelParagraphAnlage(), normService.risUrl(document),
                document.getInkrafttreten(), document.getAusserkrafttreten(),
                publication(document.getStammnormPublikationsorgan(), document.getStammnormBgblnummer()),
                publication(document.getNovellenPublikationsorgan(), document.getNovellenBgblnummer()),
                document.getWordCount(),
                includeText ? document.getFullText() : null,
                //a table of contents does not need the attachment list, only the reader of a text does
                includeText ? document.getAttachments() : null);
    }
}
