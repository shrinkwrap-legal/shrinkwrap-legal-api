package legal.shrinkwrap.api.controller;

import legal.shrinkwrap.api.dto.NormAmbiguityDto;
import legal.shrinkwrap.api.dto.NormLawDto;
import legal.shrinkwrap.api.dto.NormProvisionDto;
import legal.shrinkwrap.api.dto.NormProvisionsDto;
import legal.shrinkwrap.api.persistence.entity.NormDocumentEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import legal.shrinkwrap.api.persistence.entity.NormSectionType;
import legal.shrinkwrap.api.service.NormImporter;
import legal.shrinkwrap.api.service.NormService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    /** Matches found before the limit was applied. */
    public static final String TOTAL_COUNT = "X-Total-Count";

    /**
     * NOR40248854 for federal law, LKT40018779 and the like for the states - three letters and
     * eight digits throughout the corpus. Narrow on purpose: the segment before it, "P1" or
     * "P30a", looks alike, and a link cut off there must not pass for a document number.
     */
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile("[A-Z]{3}\\d{8}");

    private final NormService normService;

    /** Finds a law by an abbreviation or a short title, however the caller writes it. */
    @GetMapping(value = "norm/laws", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NormLawDto>> findLaws(@RequestParam("jurisdiction") String jurisdiction,
                                                     @RequestParam("query") String query,
                                                     @RequestParam(value = "limit", defaultValue = "20") int limit) {
        List<NormEntity> laws = normService.findLaws(query, jurisdiction);
        return withTotal(laws.size(), laws.stream()
                .limit(Math.min(limit, MAX_LIMIT))
                .map(this::toLawDto)
                .toList());
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
     * <p>
     * {@code risUrl} picks versions by the link an earlier answer gave for them, several separated
     * by commas; {@code docNumber} does the same with the bare RIS document number the link ends in.
     * It is the only way to one of several provisions sharing a designation - the ASVG has 24
     * "Art. 2" in force at once, one from each amending act - and the second step after a listing
     * without text. The numbers name the law, so neither jurisdiction nor gesetzesnummer is needed,
     * and they have to belong to one law. The other filters still apply; only the default of
     * today falls away, because a document number names a version rather than a day.
     */
    @GetMapping(value = "norm/provisions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NormProvisionsDto> getProvisions(
            @RequestParam(value = "jurisdiction", required = false) String jurisdiction,
            @RequestParam(value = "gesetzesnummer", required = false) String gesetzesnummer,
            @RequestParam(value = "abbreviation", required = false) String abbreviation,
            @RequestParam(value = "risUrl", required = false) List<String> risUrl,
            @RequestParam(value = "docNumber", required = false) List<String> docNumber,
            @RequestParam(value = "asOf", required = false) LocalDate asOf,
            @RequestParam(value = "inForceFrom", required = false) LocalDate inForceFrom,
            @RequestParam(value = "inForceUntil", required = false) LocalDate inForceUntil,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "typ", required = false) String typ,
            @RequestParam(value = "includeText", defaultValue = "false") boolean includeText,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        List<String> numbers = documentNumbers(risUrl, docNumber);
        boolean byNumber = !numbers.isEmpty();
        LocalDate[] period = byNumber && asOf == null && inForceFrom == null && inForceUntil == null
                ? new LocalDate[]{null, null}
                : period(asOf, inForceFrom, inForceUntil);
        NormEntity norm = byNumber
                ? lawOf(numbers, jurisdiction, gesetzesnummer, abbreviation, period)
                : namedLaw(jurisdiction, gesetzesnummer, abbreviation, period);
        List<NormDocumentEntity> found = provisions(norm, period, from, to, typ).stream()
                .filter(d -> !byNumber || numbers.contains(d.getDocNumber()))
                .toList();
        List<NormProvisionDto> page = found.stream()
                .limit(Math.min(limit, MAX_LIMIT))
                //always converted, not only when the text is wanted: the word count comes out of
                //the conversion and there is nothing in the RIS metadata to derive it from. The
                //result is stored, so this costs one fetch per provision ever, not per request
                .map(d -> toProvisionDto(normService.withText(d), includeText))
                .toList();
        return withTotal(found.size(), new NormProvisionsDto(toLawDto(norm), found.size(), page));
    }

    /**
     * A list cut off at the limit looks exactly like a complete one - 25 of the 239 TKG versions
     * of the last ten years read as the whole answer. The header says how many there were.
     */
    private static <T> ResponseEntity<T> withTotal(int total, T body) {
        return ResponseEntity.ok()
                .header(TOTAL_COUNT, String.valueOf(total))
                .cacheControl(CacheControl.maxAge(untilNextChange(ZonedDateTime.now())).cachePublic())
                .body(body);
    }

    /**
     * How long an answer stays valid. The mirror only changes in the nightly update, but "today",
     * the default of every query, moves on at midnight - a law asked for on 31 December may read
     * differently on 1 January without any update in between. So an answer is valid until
     * whichever comes first.
     */
    static Duration untilNextChange(ZonedDateTime now) {
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
        ZonedDateTime update = CronExpression.parse(NormImporter.UPDATE_CRON).next(now);
        ZonedDateTime next = update != null && update.isBefore(midnight) ? update : midnight;
        return Duration.between(now, next);
    }

    private List<NormDocumentEntity> provisions(NormEntity norm, LocalDate[] period,
                                                String from, String to, String typ) {
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

    /** A law by the name the caller gave it - which, without a document number, has to be given. */
    private NormEntity namedLaw(String jurisdiction, String gesetzesnummer, String abbreviation,
                                LocalDate[] period) {
        if (jurisdiction == null || jurisdiction.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "jurisdiction is required unless docNumber is given");
        }
        return gesetzesnummer != null
                ? law(jurisdiction, gesetzesnummer)
                : singleLawFor(jurisdiction, abbreviation, period);
    }

    /**
     * The document numbers asked for, whichever way. A link to a version ends in its number -
     * {@code …/eli/bgbl/i/2022/196/P1/NOR40248854} - so that is all that is taken from it.
     */
    private static List<String> documentNumbers(List<String> risUrls, List<String> docNumbers) {
        List<String> numbers = new ArrayList<>();
        for (String url : risUrls == null ? List.<String>of() : risUrls) {
            String path = url.trim().replaceFirst("[?#].*$", "").replaceFirst("/+$", "");
            String number = path.substring(path.lastIndexOf('/') + 1);
            if (!DOCUMENT_NUMBER.matcher(number).matches()) {
                throw new ResponseStatusException(BAD_REQUEST, "no document number at the end of " + url);
            }
            numbers.add(number);
        }
        for (String number : docNumbers == null ? List.<String>of() : docNumbers) {
            numbers.add(number.trim());
        }
        return numbers;
    }

    /**
     * The law the document numbers belong to. One answer carries one law, so numbers from several
     * laws are a request to split. A law named alongside has to be that same law.
     */
    private NormEntity lawOf(List<String> docNumbers, String jurisdiction, String gesetzesnummer,
                             String abbreviation, LocalDate[] period) {
        List<NormDocumentEntity> documents = docNumbers.stream()
                .map(number -> normService.findByDocNumber(number)
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "unknown document " + number)))
                .toList();
        if (documents.stream().map(d -> d.getNorm().getId()).distinct().count() > 1) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "the document numbers belong to different laws - one request per law");
        }
        NormEntity law = normService.lawOf(documents.getFirst());
        if (gesetzesnummer != null || abbreviation != null) {
            NormEntity named = namedLaw(jurisdiction != null ? jurisdiction : law.getJurisdiction(),
                    gesetzesnummer, abbreviation, period);
            if (!named.getId().equals(law.getId())) {
                throw new ResponseStatusException(BAD_REQUEST, "the document numbers do not belong to "
                        + (gesetzesnummer != null ? gesetzesnummer : abbreviation));
            }
        }
        return law;
    }

    private NormEntity law(String jurisdiction, String gesetzesnummer) {
        return normService.findLaw(jurisdiction, gesetzesnummer)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                        "unknown law " + gesetzesnummer + " in " + jurisdiction));
    }

    /**
     * An abbreviation can belong to several laws. Rather than picking one, the caller is told to
     * disambiguate - which of the two "UStG 1994" is meant is not something to guess at.
     * <p>
     * The period narrows it first. "TKG" names TKG 2003 and TKG 2021 alike, but asked for today's
     * law only one of them has provisions; asked for 2015 only the other. Only candidates that
     * all have provisions in the period - or none of which has - are left to the caller. A single
     * candidate is taken as it is, even without provisions then: the name was unambiguous, and an
     * empty list says what there is to say.
     */
    private NormEntity singleLawFor(String jurisdiction, String abbreviation, LocalDate[] period) {
        if (abbreviation == null) {
            throw new ResponseStatusException(NOT_FOUND, "either gesetzesnummer or abbreviation is required");
        }
        List<NormEntity> laws = normService.findLaws(abbreviation, jurisdiction);
        if (laws.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "no law found for " + abbreviation);
        }
        if (laws.size() == 1) {
            return laws.getFirst();
        }
        List<NormEntity> inPeriod = normService.withProvisionsBetween(laws, period[0], period[1]);
        if (inPeriod.size() == 1) {
            return inPeriod.getFirst();
        }
        throw new AmbiguousName(abbreviation, inPeriod.isEmpty() ? laws : inPeriod);
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
            //the handler below builds the REST answer from the fields; the message is what a tool
            //call sees, so it has to carry the candidates on its own
            super("\"" + name + "\" names " + laws.size() + " laws - repeat the request with the "
                    + "gesetzesnummer of the one that is meant: " + laws.stream()
                    .limit(MAX_CANDIDATES)
                    .map(n -> n.getIdentifier() + " (" + n.getKurztitel() + ")")
                    .collect(Collectors.joining(", ")));
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
                document.getArtikelParagraphAnlage(), document.getUebergangsrecht(), normService.risUrl(document),
                document.getInkrafttreten(), document.getAusserkrafttreten(),
                publication(document.getStammnormPublikationsorgan(), document.getStammnormBgblnummer()),
                publication(document.getNovellenPublikationsorgan(), document.getNovellenBgblnummer()),
                document.getWordCount(),
                includeText ? document.getFullText() : null,
                //a table of contents does not need the attachment list, only the reader of a text does
                includeText ? document.getAttachments() : null);
    }
}
