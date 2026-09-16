package legal.shrinkwrap.api.service;

import at.gv.bka.ris.v26.soap.ws.client.Bundesland;
import at.gv.bka.ris.v26.soap.ws.client.NormDokumenttyp;
import at.gv.bka.ris.v26.soap.ws.client.NormabschnittTyp;
import jakarta.annotation.PostConstruct;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterNorm;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormAttachment;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormResult;
import legal.shrinkwrap.api.persistence.entity.NormAbbreviationEntity;
import legal.shrinkwrap.api.persistence.entity.NormAbbreviationSource;
import legal.shrinkwrap.api.persistence.entity.NormDocumentEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import legal.shrinkwrap.api.persistence.entity.NormSectionType;
import legal.shrinkwrap.api.persistence.entity.NormSource;
import legal.shrinkwrap.api.persistence.repo.NormAbbreviationRepository;
import legal.shrinkwrap.api.persistence.repo.NormDocumentRepository;
import legal.shrinkwrap.api.persistence.repo.NormRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.Set;

/**
 * Imports the metadata of a law: every provision in every version it ever had.
 * <p>
 * Text is deliberately not fetched here. The mirror is complete in its metadata and demand
 * driven in its text, so a caller can see what exists without the import having pulled
 * hundreds of documents that nobody will read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NormImporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Beyond this a short title is a sentence, not a name - "VfGH-Ausspruch, dass § 1 der
     * Verordnung des Landeshauptmanns von Tirol …" runs to 267 characters, the longest in the
     * corpus to 539. Nobody types those, so they do not become lookup keys; the laws stay
     * findable through the title search, which is what the trigram indexes are for.
     */
    private static final int MAX_NAME_LENGTH = 200;

    /** Accepts every type, so the property may stay untouched for a complete mirror. */
    private static final String ALL_TYPES = "*";

    /** Several days, so a run that fails or is skipped is caught by the next one. */
    private static final int CHANGE_WINDOW_DAYS = 5;

    /** Nulls last, so a version RIS left undated never wins a min() over the dated ones. */
    private static final Comparator<RisNormResult> BY_INKRAFTTRETEN =
            Comparator.comparing(r -> r.getNormMetadaten().getInkrafttreten(),
                    Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * Which laws the import takes, matched against the type RIS writes on every document: "BG"
     * for a federal act, "LG" for a state one, "V" for a regulation. "*" takes everything and is
     * what production runs; a staging system narrows it to the acts, which are a fifth of the
     * laws. It applies to the nightly update too, or the change feed would put back what the
     * initial import left out.
     */
    @Value("${ris.norm.import-types:*}")
    String importTypes;

    /**
     * How far back repealed laws are taken, by the day they went out of force. TKG 2003 and the
     * Kesselgesetz no longer apply but decide cases still pending; a law that ended in 1950 rarely
     * does. Empty takes every repealed law. Only the initial import reads it - a repealed law
     * changes too seldom for the change feed to need the same restriction.
     */
    @Value("${ris.norm.repealed-since:2000-01-01}")
    String repealedSince;

    private final RisSoapAdapter risSoapAdapter;
    private final NormRepository normRepository;
    private final NormDocumentRepository normDocumentRepository;
    private final NormAbbreviationRepository normAbbreviationRepository;

    /**
     * The transaction wraps one law when this is called from outside. {@link #importAllLaws}
     * calls it from within this bean, where Spring's proxy is bypassed and every row therefore
     * commits on its own - which is what a bulk run wants: an interrupted import leaves what it
     * managed to write, and {@link #importAllLaws} picks up from there.
     *
     * @param bundesland     null imports federal law, a value imports that state's law
     * @param gesetzesnummer the RIS law number - the only criterion that reliably brings up a
     *                       complete law
     */
    @Transactional
    public NormEntity importLaw(Bundesland bundesland, String gesetzesnummer) {
        List<RisNormResult> results = risSoapAdapter.findNormDocuments(RisSearchParameterNorm.builder()
                .bundesland(bundesland)
                .gesetzesnummer(gesetzesnummer)
                .build());

        if (results.isEmpty()) {
            log.info("no documents for Gesetzesnummer {}", gesetzesnummer);
            return null;
        }

        //has to exist before the documents that reference it, or the first import of a law
        //fails on a transient instance
        NormEntity norm = normRepository.save(findOrCreate(bundesland, gesetzesnummer));

        //the order RIS returns is the only usable one, so the position in the list is the order
        int position = 0;
        Set<String> delivered = new HashSet<>();
        for (RisNormResult result : results) {
            storeDocument(norm, result, position++);
            delivered.add(result.getMetadaten().getId());
        }
        markWithdrawn(norm, delivered);

        applyCurrentTitle(norm, results);
        norm.setStructureLoadedFor(LocalDate.now());
        norm.setLastChangeCheck(LocalDateTime.now());
        normRepository.save(norm);

        log.info("imported {} documents for Gesetzesnummer {} ({})",
                results.size(), gesetzesnummer, norm.getKurztitel());
        return norm;
    }

    /**
     * Marks the provisions RIS stopped delivering for this law as gone. They are kept rather
     * than removed, so a decision citing them decades from now still resolves.
     * <p>
     * The comparison is against the whole law on purpose: a withdrawal is never announced. RIS
     * reports a deleted document in the change feed as an entry without any payload at all, so
     * the only way to notice one is that it is missing from a complete result. Which also means
     * this is only correct after a complete import - never after a narrowed query.
     */
    private void markWithdrawn(NormEntity norm, Set<String> delivered) {
        List<String> withdrawn = normDocumentRepository.findLiveDocNumbers(norm).stream()
                .filter(docNumber -> !delivered.contains(docNumber))
                .toList();
        if (withdrawn.isEmpty()) {
            return;
        }
        normDocumentRepository.markDeleted(withdrawn, LocalDateTime.now());
        log.info("{} provisions of Gesetzesnummer {} are no longer delivered by RIS: {}",
                withdrawn.size(), norm.getIdentifier(), withdrawn);
    }

    /**
     * Harvests the RIS change feed and re-imports every law it touched. A changed provision is
     * not imported on its own: the sort order comes from the position in the complete result, so
     * a law is only ever written as a whole.
     */
    @Scheduled(cron = "0 15 2 * * *")
    public void updateChangedLaws() {
        for (boolean landesrecht : new boolean[]{false, true}) {
            List<RisNormResult> changed;
            try {
                changed = risSoapAdapter.findChangedNormDocuments(landesrecht, CHANGE_WINDOW_DAYS);
            } catch (RuntimeException e) {
                log.error("change feed for {} failed: {}",
                        landesrecht ? "Landesnormen" : "Bundesnormen", e.getMessage());
                continue;
            }

            Map<LawKey, List<RisNormResult>> byLaw = new LinkedHashMap<>();
            for (RisNormResult result : changed) {
                String gesetzesnummer = result.getNormMetadaten().getGesetzesnummer();
                if (gesetzesnummer == null) {
                    continue;
                }
                if (!importsType(result.getNormMetadaten().getTyp())) {
                    continue;
                }
                Bundesland state = bundeslandOf(result.getNormMetadaten().getBundesland());
                //a null state is the key of federal law, so an unreadable one would file a state
                //law under the federal law of the same Gesetzesnummer - the numbers are not
                //unique across the two. Better to skip it loudly than to write it to the wrong law
                if (landesrecht && state == null) {
                    log.error("changed document {} reports the unknown Bundesland {}, skipped",
                            result.getMetadaten().getId(), result.getNormMetadaten().getBundesland());
                    continue;
                }
                byLaw.computeIfAbsent(new LawKey(state, gesetzesnummer), key -> new ArrayList<>())
                        .add(result);
            }
            log.info("{} changed documents in {} laws", changed.size(), byLaw.size());

            byLaw.forEach((key, results) -> {
                try {
                    applyChanges(key.bundesland(), key.gesetzesnummer(), results);
                } catch (RuntimeException e) {
                    //one failing law must not take the whole nightly run with it
                    log.error("update of Gesetzesnummer {} failed: {}", key.gesetzesnummer(), e.getMessage());
                }
            });
        }
    }

    /**
     * Writes the changes of one law. Updating the changed documents in place is enough as long
     * as the law keeps the shape we already know: the order of the provisions comes from their
     * position in the complete result, so it cannot be derived from a handful of documents.
     * <p>
     * A provision we have never seen means the shape did change, and then the whole law is
     * fetched again - which also gives {@link #markWithdrawn} the chance to notice what has
     * disappeared. That matters, because RIS reports a withdrawn document as an empty entry the
     * adapter has to drop, so a deletion is never visible in the feed itself.
     * <p>
     * The difference is not cosmetic: the ASVG holds 5115 documents over all its versions, and
     * refetching them because one paragraph was amended would be the normal case, not the
     * exception.
     */
    @Transactional
    public void applyChanges(Bundesland bundesland, String gesetzesnummer, List<RisNormResult> changed) {
        NormEntity norm = findLaw(bundesland, gesetzesnummer).orElse(null);

        boolean shapeChanged = norm == null || changed.stream()
                .anyMatch(r -> normDocumentRepository.findByDocNumber(r.getMetadaten().getId()).isEmpty());

        if (shapeChanged) {
            importLaw(bundesland, gesetzesnummer);
            return;
        }

        for (RisNormResult result : changed) {
            storeDocument(norm, result, null);
        }
        applyCurrentTitle(norm, changed);
        norm.setLastChangeCheck(LocalDateTime.now());
        normRepository.save(norm);
        log.info("updated {} documents of Gesetzesnummer {} in place", changed.size(), gesetzesnummer);
    }

    private record LawKey(Bundesland bundesland, String gesetzesnummer) {
    }

    /**
     * Whether a law of this type is imported. The wildcard and an empty setting take everything,
     * so leaving the property alone keeps the behaviour of a complete mirror. Matching ignores
     * case and spacing: the state type is free text, and RIS writes "LG", "Gesetz" and " K " alike.
     */
    boolean importsType(String typ) {
        if (importTypes == null || importTypes.isBlank() || ALL_TYPES.equals(importTypes.trim())) {
            return true;
        }
        String wanted = typ == null ? "" : typ.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(importTypes.split(","))
                .map(accepted -> accepted.trim().toLowerCase(Locale.ROOT))
                .anyMatch(wanted::equals);
    }

    /**
     * Whether a head is recent enough. One head per version comes back, so a law whose early head
     * ended long ago is still taken through the head in force today; one without an end date is
     * either in force or not yet, and taken either way.
     */
    boolean importsPeriod(RisNormMetadaten head) {
        if (repealedSince == null || repealedSince.isBlank() || head.getAusserkrafttreten() == null) {
            return true;
        }
        return !head.getAusserkrafttreten().isBefore(LocalDate.parse(repealedSince.trim()));
    }

    /**
     * RIS reports the state as its plain name in the response; the request needs the enum.
     * <p>
     * The two spellings do not match: the schema transliterates - {@code Oberoesterreich} - while
     * the response carries the umlaut. Without folding them, Kaernten, Niederoesterreich and
     * Oberoesterreich never resolve, and a null state means federal law one line further down.
     */
    static Bundesland bundeslandOf(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        //exactly three of the nine names carry one, and never as the first letter
        String transliterated = name.trim().replace("ä", "ae").replace("ö", "oe");
        try {
            return Bundesland.fromValue(transliterated);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Every law of a jurisdiction, by way of its head document.
     * <p>
     * RIS numbers the head document of a law "§ 0" and marks it {@code Dokumenttyp: Norm} - also
     * for laws divided into articles, the B-VG included. Asking for § 0 yields one document per
     * version of every head, which is a far cheaper way to enumerate than paging the corpus:
     * 26.011 heads of 21.962 federal laws against 441.147 documents.
     * <p>
     * Deliberately without a day. Asked for as they stand today, the heads leave out every law
     * that has been repealed - 11.275 in federal law alone.
     * <p>
     * Only over SOAP. The same request over REST is wrong - it drops the Bundesland restriction
     * without a word and answers with all 8.350 state laws.
     */
    public List<String> findLawNumbers(Bundesland bundesland) {
        return risSoapAdapter.findNormDocuments(RisSearchParameterNorm.builder()
                        .bundesland(bundesland)
                        .abschnitt(NormabschnittTyp.PARAGRAPH, "0", "0")
                        .build()).stream()
                .map(RisNormResult::getNormMetadaten)
                //the head already carries type and end date, so a law left out is never fetched
                .filter(metadaten -> importsType(metadaten.getTyp()) && importsPeriod(metadaten))
                .map(RisNormMetadaten::getGesetzesnummer)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Imports every law of a jurisdiction that is not already there, so an interrupted run
     * carries on where it stopped instead of writing everything a second time. Over 19.000 laws
     * that matters: the run is expected to span more than one day.
     *
     * @return how many laws were newly imported; a law that fails is logged and skipped
     */
    public int importAllLaws(Bundesland bundesland) {
        String jurisdiction = jurisdictionOf(bundesland);
        List<String> lawNumbers = findLawNumbers(bundesland);
        log.info("initial import: {} laws for {}", lawNumbers.size(), jurisdiction);

        int imported = 0;
        int skipped = 0;
        for (String gesetzesnummer : lawNumbers) {
            if (alreadyImported(jurisdiction, gesetzesnummer)) {
                skipped++;
                continue;
            }
            try {
                importLaw(bundesland, gesetzesnummer);
                imported++;
            } catch (RuntimeException e) {
                log.error("initial import of Gesetzesnummer {} failed: {}", gesetzesnummer, e.getMessage());
            }
            //without the guard this fires on every attempt while nothing has been imported yet,
            //which reads like a run that is stuck rather than one that keeps failing
            if (imported > 0 && imported % 100 == 0) {
                log.info("initial import: {} imported, {} already present, of {} laws for {}",
                        imported, skipped, lawNumbers.size(), jurisdiction);
            }

        }
        log.info("initial import finished for {}: {} imported, {} already present",
                jurisdiction, imported, skipped);
        return imported;
    }

    /** Federal law and all nine states. Runs for hours and writes some 722.000 documents. */
    //@PostConstruct
    public void importEverything() {
        new Thread(() -> {
            importAllLaws(null);
            for (Bundesland bundesland : Bundesland.values()) {
                if (bundesland != Bundesland.UNDEFINED) {
                    importAllLaws(bundesland);
                }
            }
            ;
        }).start();
    }

    /**
     * Whether the law has been loaded at all - not whether it was loaded today. The initial
     * import is expected to run across several days, so a same-day check would treat everything
     * from yesterday as missing and fetch it all over again. Keeping the mirror current is the
     * change feed's job, not this one's.
     */
    private boolean alreadyImported(String jurisdiction, String gesetzesnummer) {
        return normRepository.findByJurisdictionAndIdentifier(jurisdiction, gesetzesnummer)
                .map(NormEntity::getStructureLoadedFor)
                .isPresent();
    }

    private Optional<NormEntity> findLaw(Bundesland bundesland, String gesetzesnummer) {
        return normRepository.findByJurisdictionAndSourceAndIdentifier(
                jurisdictionOf(bundesland), sourceOf(bundesland).name(), gesetzesnummer);
    }

    private NormEntity findOrCreate(Bundesland bundesland, String gesetzesnummer) {
        return findLaw(bundesland, gesetzesnummer).orElseGet(() -> {
            NormEntity created = new NormEntity();
            created.setJurisdiction(jurisdictionOf(bundesland));
            created.setSource(sourceOf(bundesland).name());
            created.setIdentifier(gesetzesnummer);
            return created;
        });
    }

    private static NormSource sourceOf(Bundesland bundesland) {
        return bundesland == null ? NormSource.RIS_BR : NormSource.RIS_LR;
    }

    /**
     * @param position the place in the complete result, or null to leave the stored order alone -
     *                 the order is only meaningful when the whole law was fetched at once
     */
    private NormDocumentEntity storeDocument(NormEntity norm, RisNormResult result, Integer position) {
        RisNormMetadaten source = result.getNormMetadaten();
        String docNumber = result.getMetadaten().getId();

        NormDocumentEntity document = normDocumentRepository.findByDocNumber(docNumber)
                .orElseGet(NormDocumentEntity::new);

        document.setDocNumber(docNumber);
        document.setNorm(norm);
        if (position != null) {
            document.setSortIndex(position);
        }
        document.setEliProvision(eliProvisionPath(source.getEli()));

        NormSectionType sectionType = sectionTypeOf(source);
        document.setAbschnittTyp(sectionType.name());
        document.setArtikelParagraphAnlage(source.getArtikelParagraphAnlage());
        document.setNummer(nummerOf(source, sectionType));
        document.setBuchstabe(buchstabeOf(source, sectionType));
        document.setTeil(source.getAnlagenteil());

        document.setInkrafttreten(source.getInkrafttreten());
        document.setAusserkrafttreten(source.getAusserkrafttreten());
        document.setStammnormPublikationsorgan(source.getStammnormPublikationsorgan());
        document.setStammnormBgblnummer(source.getStammnormBgblnummer());
        document.setNovellenPublikationsorgan(source.getNovellenPublikationsorgan());
        document.setNovellenBgblnummer(source.getNovellenBgblnummer());
        document.setNovellenBeziehung(source.getNovellenBeziehung());
        document.setUebergangsrecht(stripped(source.getUebergangsrecht()));
        document.setBeachte(source.getBeachte());
        document.setAnmerkung(source.getAnmerkung());
        //RIS also rewrites a document in place, keeping its docNumber - the ABGB head is reissued
        //that way whenever its table of contents moves. The cached text then belongs to content
        //that is gone, and nothing else would ever throw it away
        if (!Objects.equals(document.getGeaendert(), result.getMetadaten().getChanged())) {
            document.setFullText(null);
            document.setWordCount(null);
            document.setTextConversionVersion(null);
        }
        document.setGeaendert(result.getMetadaten().getChanged());
        document.setHtmlUrl(result.getHtmlDocumentUrl());
        document.setMetadata(result.getMetadaten().getFullResponseAsJson());
        document.setAttachments(asJson(result.getAttachments()));

        //a document RIS delivers again is not deleted, whatever a previous run concluded
        document.setDeletedAt(null);

        document = normDocumentRepository.save(document);
        return document;
    }

    /**
     * Title and abbreviation are current values without history, but historical versions carry
     * the wording of their day. A plain "last write wins" would cement whichever document came
     * last, so only a version at least as new as the one behind the stored values may overwrite.
     * <p>
     * The long title and the ELI of the law are not in that group: RIS carries them on the head
     * document alone, every other provision leaves them empty. They get their own source.
     */
    private void applyCurrentTitle(NormEntity norm, List<RisNormResult> results) {
        LocalDate today = LocalDate.now();
        applyHead(norm, results, today);

        RisNormResult newest = titleSource(norm, results, today);
        if (newest == null) {
            return;
        }

        RisNormMetadaten source = newest.getNormMetadaten();
        LocalDate from = source.getInkrafttreten();
        if (norm.getTitleSourceInkrafttreten() != null && from != null
                && from.isBefore(norm.getTitleSourceInkrafttreten())) {
            return;
        }

        norm.setKurztitel(stripped(source.getKurztitel()));
        norm.setTyp(stripped(source.getTyp()));
        norm.setKundmachungsorgan(stripped(source.getKundmachungsorgan()));
        norm.setGesamteRechtsvorschriftUrl(source.getGesamteRechtsvorschriftUrl());
        norm.setIndizes(source.getIndizes() == null ? null : String.join("; ", source.getIndizes()));
        //the law may not be in force yet, and then the fallback above handed out a future date
        norm.setTitleSourceInkrafttreten(inForceAlready(source, today) ? from : null);

        storeName(norm, source.getAbkuerzung(), NormAbbreviationSource.BRKONS);
        storeName(norm, source.getKurztitel(), NormAbbreviationSource.KURZTITEL);
    }

    /**
     * The version the current values are read from: the newest one that has come into force.
     * <p>
     * A version still to come must not be it. It would push {@code titleSourceInkrafttreten} into
     * the future, and the guard in the caller would then turn down every later change until that
     * date arrives - including RIS' 9000-01-01 for "never". Their order among themselves says
     * nothing either: an amendment resolved later may well start earlier than one resolved
     * before it, so the last one RIS wrote is not the one that will apply first.
     *
     * @return null when nothing has come into force and the law already carries values - then
     * there is nothing current to say and what is stored stays
     */
    static RisNormResult titleSource(NormEntity norm, List<RisNormResult> results, LocalDate today) {
        return results.stream()
                .filter(r -> inForceAlready(r.getNormMetadaten(), today))
                .max(BY_INKRAFTTRETEN)
                //a law before its own start has only future versions to offer, and a row with
                //nothing in it is better served by the one starting first than by nothing at all
                .or(() -> norm.getKurztitel() != null
                        ? Optional.<RisNormResult>empty()
                        : results.stream().min(BY_INKRAFTTRETEN))
                .orElse(null);
    }

    /**
     * Takes the long title and the ELI from the head document, which is the only one carrying
     * them. Only the head that is in force may write: RIS keeps the superseded ones and there is
     * exactly one current at a time, so a correction to an old version must not rewrite the
     * title. A run without a head is a run in which the head did not change - a new one brings a
     * new docNumber and with it a complete reimport.
     */
    static void applyHead(NormEntity norm, List<RisNormResult> results, LocalDate today) {
        results.stream()
                .map(RisNormResult::getNormMetadaten)
                .filter(m -> NormDokumenttyp.NORM.equals(m.getDokumenttyp()))
                .filter(m -> inForceAlready(m, today)
                        && (m.getAusserkrafttreten() == null || m.getAusserkrafttreten().isAfter(today)))
                .findFirst()
                .ifPresent(head -> {
                    norm.setLangtitel(stripped(head.getTitel()));
                    norm.setEli(head.getEli());
                });
    }

    /**
     * RIS pads what it delivers - "Allgemeines Pensionsgesetz ", and the Kundmachungsorgan ends in
     * a space almost every time. Only the edges go; markup inside, such as the line breaks of the
     * long title, is content.
     */
    static String stripped(String value) {
        return value == null ? null : value.strip();
    }

    static boolean inForceAlready(RisNormMetadaten metadaten, LocalDate today) {
        return metadaten.getInkrafttreten() != null && !metadaten.getInkrafttreten().isAfter(today);
    }

    /**
     * Names accumulate and are never dropped - decisions go on citing with the abbreviation of
     * their day for decades, so a name seen once stays findable. Abbreviation and short title
     * share the table, so a query does not have to know which of the two it was given.
     */
    private void storeName(NormEntity norm, String written, NormAbbreviationSource quelle) {
        String value = stripped(written);
        //measured against the value as written, not against the normalised key: condensing
        //drops dots and spaces, so a 267 character title shrinks to 223 and would slip past a
        //check on the key while the untouched original still goes into the row
        if (value == null || value.length() > MAX_NAME_LENGTH) {
            return;
        }
        for (String normalized : NormAbbreviations.normalize(value)) {
            NormAbbreviationEntity entity = normAbbreviationRepository
                    .findByNormAndNormalized(norm, normalized)
                    .orElseGet(() -> {
                        NormAbbreviationEntity created = new NormAbbreviationEntity();
                        created.setNorm(norm);
                        created.setNormalized(normalized);
                        created.setQuelle(quelle.name());
                        return created;
                    });
            entity.setAbkuerzung(value);
            entity.setLastSeen(LocalDateTime.now());
            normAbbreviationRepository.save(entity);
        }
    }

    /** ISO 3166-2, in the order the RIS enum happens to use. */
    public static String jurisdictionOf(Bundesland bundesland) {
        if (bundesland == null) {
            return "AT";
        }
        return switch (bundesland) {
            case BURGENLAND -> "AT-1";
            case KAERNTEN -> "AT-2";
            case NIEDEROESTERREICH -> "AT-3";
            case OBEROESTERREICH -> "AT-4";
            case SALZBURG -> "AT-5";
            case STEIERMARK -> "AT-6";
            case TIROL -> "AT-7";
            case VORARLBERG -> "AT-8";
            case WIEN -> "AT-9";
            case UNDEFINED -> "AT";
        };
    }

    /**
     * RIS does not state the kind directly, it is implied by which number field is filled.
     * {@code Dokumenttyp: Norm} marks the head document of a law, which RIS numbers "§ 0" and
     * which would otherwise be mistaken for a paragraph.
     * <p>
     * The paragraph is checked before the article, because RIS fills <em>both</em> for a
     * provision designated "Art. 4 § 1" - and the § is what a citation names. Whole laws are
     * built this way: the Datenschutzgesetz has 76 such provisions, the Angestelltengesetz 42.
     * Reading them as articles loses the § entirely and makes every paragraph of an article
     * collapse onto its article number. The article stays visible in
     * {@link RisNormMetadaten#getArtikelParagraphAnlage()}.
     */
    static NormSectionType sectionTypeOf(RisNormMetadaten metadaten) {
        if (NormDokumenttyp.NORM.equals(metadaten.getDokumenttyp())) {
            return NormSectionType.NORM;
        }
        if (metadaten.getAnlagennummer() != null || metadaten.getAnlagenbuchstabe() != null
                || metadaten.getAnlagenteil() != null) {
            return NormSectionType.ANLAGE;
        }
        if (metadaten.getParagraphnummer() != null || metadaten.getParagraphbuchstabe() != null) {
            return NormSectionType.PARAGRAPH;
        }
        if (metadaten.getArtikelnummer() != null || metadaten.getArtikelbuchstabe() != null) {
            return NormSectionType.ARTIKEL;
        }
        return NormSectionType.PARAGRAPH;
    }

    static String nummerOf(RisNormMetadaten metadaten, NormSectionType sectionType) {
        return switch (sectionType) {
            case ANLAGE -> metadaten.getAnlagennummer();
            case ARTIKEL -> asString(metadaten.getArtikelnummer());
            case PARAGRAPH, NORM -> asString(metadaten.getParagraphnummer());
        };
    }

    static String buchstabeOf(RisNormMetadaten metadaten, NormSectionType sectionType) {
        return switch (sectionType) {
            case ANLAGE -> metadaten.getAnlagenbuchstabe();
            case ARTIKEL -> metadaten.getArtikelbuchstabe();
            case PARAGRAPH, NORM -> metadaten.getParagraphbuchstabe();
        };
    }

    /**
     * The ELI comes with a changing host - ris.bka.gv.at, www.ris.bka.gv.at, ogd.ris.bka.gv.at -
     * so only the path is kept. Dropping the trailing {@code /NOR…} turns the identifier of one
     * document into the identifier of the provision across all its versions.
     */
    static String eliProvisionPath(String eli) {
        if (eli == null || eli.isBlank()) {
            return null;
        }
        String path = eli.trim();
        int scheme = path.indexOf("://");
        if (scheme >= 0) {
            int firstSlash = path.indexOf('/', scheme + 3);
            path = firstSlash < 0 ? "" : path.substring(firstSlash);
        }
        path = path.replaceFirst("/NOR\\d+$", "");
        return path.isBlank() ? null : path;
    }

    /** Null rather than "[]" for the ordinary case, so an empty column stays readable. */
    private static String asJson(List<RisNormAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attachments);
        } catch (JsonProcessingException e) {
            log.error("could not serialise attachments: {}", e.getMessage());
            return null;
        }
    }

    private static String asString(Integer value) {
        return value == null ? null : String.valueOf(value);
    }
}
