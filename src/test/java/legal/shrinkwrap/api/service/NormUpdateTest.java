package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.SpringTest;
import legal.shrinkwrap.api.adapter.ris.dto.RisMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormResult;
import legal.shrinkwrap.api.persistence.entity.NormDocumentEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import legal.shrinkwrap.api.persistence.repo.NormDocumentRepository;
import legal.shrinkwrap.api.persistence.repo.NormRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What an amendment does to a law that is already stored. The change feed delivers the amended
 * document under the document number it always had, so everything here turns on the existing
 * row being updated rather than a second one appearing beside it.
 */
@Tag("network")
@SpringBootTest
public class NormUpdateTest extends SpringTest {

    private static final String BPRBG = "20012101";
    private static final String PARAGRAPH_1 = "NOR40248854";

    @Autowired
    private NormImporter normImporter;

    @Autowired
    private NormService normService;

    @Autowired
    private NormRepository normRepository;

    @Autowired
    private NormDocumentRepository normDocumentRepository;

    private NormEntity norm;

    @BeforeEach
    public void importOnce() {
        norm = normImporter.importLaw(null, BPRBG);
    }

    /** An amendment that gives a provision an end date it did not have before. */
    @Test
    public void newlySetFieldsReachTheStoredProvision() {
        NormDocumentEntity before = normDocumentRepository.findByDocNumber(PARAGRAPH_1).orElseThrow();
        assertThat(before.getAusserkrafttreten()).isNull();
        assertThat(before.getAnmerkung()).isNull();
        Integer sortIndexBefore = before.getSortIndex();
        long documentsBefore = normDocumentRepository.count();

        RisNormMetadaten amended = new RisNormMetadaten();
        amended.setGesetzesnummer(BPRBG);
        amended.setKurztitel(norm.getKurztitel());
        amended.setArtikelParagraphAnlage("§ 1");
        amended.setParagraphnummer(1);
        amended.setInkrafttreten(before.getInkrafttreten());
        amended.setAusserkrafttreten(LocalDate.of(2027, 12, 31));
        amended.setAnmerkung("durch Novelle befristet");

        normImporter.applyChanges(null, BPRBG, List.of(resultFor(amended, before)));

        NormDocumentEntity after = normDocumentRepository.findByDocNumber(PARAGRAPH_1).orElseThrow();
        assertThat(after.getAusserkrafttreten()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(after.getAnmerkung()).isEqualTo("durch Novelle befristet");
        assertThat(after.getDeletedAt()).isNull();

        //the order is not re-derived from a partial result, so it has to survive untouched
        assertThat(after.getSortIndex()).isEqualTo(sortIndexBefore);
        assertThat(normDocumentRepository.count()).isEqualTo(documentsBefore);
    }

    /**
     * Titles change while the document number stays. The current value follows, the old name
     * stays a lookup key - decisions go on citing with the wording of their day.
     */
    @Test
    public void changedTitleIsAdoptedAndTheOldNameKeepsResolving() {
        NormDocumentEntity before = normDocumentRepository.findByDocNumber(PARAGRAPH_1).orElseThrow();
        assertThat(norm.getKurztitel()).startsWith("Buchpreisbindungsgesetz");

        RisNormMetadaten renamed = new RisNormMetadaten();
        renamed.setGesetzesnummer(BPRBG);
        renamed.setKurztitel("Buchpreisbindungsgesetz 2028");
        renamed.setAbkuerzung("BPrBG 2028");
        renamed.setArtikelParagraphAnlage("§ 1");
        renamed.setParagraphnummer(1);
        renamed.setInkrafttreten(before.getInkrafttreten());

        normImporter.applyChanges(null, BPRBG, List.of(resultFor(renamed, before)));

        NormEntity reloaded = normRepository.findByJurisdictionAndIdentifier("AT", BPRBG).orElseThrow();
        assertThat(reloaded.getKurztitel()).isEqualTo("Buchpreisbindungsgesetz 2028");
        assertThat(reloaded.getId()).isEqualTo(norm.getId());

        assertThat(normService.findLaws("Buchpreisbindungsgesetz 2028", "AT")).isNotEmpty();
        assertThat(normService.findLaws("BPrBG 2028", "AT")).isNotEmpty();
        assertThat(normService.findLaws("BPrBG 2023", "AT")).isNotEmpty();
        assertThat(normService.namesOf(reloaded))
                .contains("BPrBG 2023", "BPrBG 2028", "Buchpreisbindungsgesetz 2028");
    }

    /**
     * RIS does not only add versions, it also rewrites a document in place and keeps its number -
     * the ABGB head is reissued that way whenever its table of contents moves, most recently on
     * 20 August 2026 for a provision in force since 1812. The stored text then describes content
     * that is gone, and nothing but the change date would ever notice.
     */
    @Test
    public void aRewrittenDocumentLosesItsCachedText() {
        NormDocumentEntity stored = normDocumentRepository.findByDocNumber(PARAGRAPH_1).orElseThrow();

        //fill the cache first - the text is loaded on demand and on nobody else's account
        NormDocumentEntity cached = normService.withText(stored);
        assertThat(cached.getFullText()).isNotBlank();
        assertThat(cached.getWordCount()).isGreaterThan(0L);
        assertThat(cached.getTextConversionVersion()).isEqualTo(NormService.TEXT_CONVERSION_VERSION);

        RisNormMetadaten rewritten = new RisNormMetadaten();
        rewritten.setGesetzesnummer(BPRBG);
        rewritten.setKurztitel(norm.getKurztitel());
        rewritten.setArtikelParagraphAnlage("§ 1");
        rewritten.setParagraphnummer(1);
        rewritten.setInkrafttreten(stored.getInkrafttreten());

        LocalDate laterThanBefore = cached.getGeaendert() == null
                ? LocalDate.now() : cached.getGeaendert().plusDays(1);
        normImporter.applyChanges(null, BPRBG,
                List.of(resultFor(rewritten, stored, laterThanBefore)));

        NormDocumentEntity after = normDocumentRepository.findByDocNumber(PARAGRAPH_1).orElseThrow();
        assertThat(after.getGeaendert()).isEqualTo(laterThanBefore);
        assertThat(after.getFullText()).isNull();
        assertThat(after.getWordCount()).isNull();
        assertThat(after.getTextConversionVersion()).isNull();

        //and it is the absence of the text that makes the next read fetch it again
        assertThat(normService.withText(after).getFullText()).isNotBlank();
    }

    /**
     * The feed re-delivers documents that did not change, and the window is five days wide on
     * purpose. Dropping the text for those would refetch and re-convert whole laws every night.
     */
    @Test
    public void aDocumentDeliveredUnchangedKeepsItsCachedText() {
        NormDocumentEntity stored = normDocumentRepository.findByDocNumber(PARAGRAPH_1).orElseThrow();
        NormDocumentEntity cached = normService.withText(stored);
        String text = cached.getFullText();
        assertThat(text).isNotBlank();

        RisNormMetadaten unchanged = new RisNormMetadaten();
        unchanged.setGesetzesnummer(BPRBG);
        unchanged.setKurztitel(norm.getKurztitel());
        unchanged.setArtikelParagraphAnlage("§ 1");
        unchanged.setParagraphnummer(1);
        unchanged.setInkrafttreten(stored.getInkrafttreten());

        normImporter.applyChanges(null, BPRBG,
                List.of(resultFor(unchanged, stored, cached.getGeaendert())));

        NormDocumentEntity after = normDocumentRepository.findByDocNumber(PARAGRAPH_1).orElseThrow();
        assertThat(after.getFullText()).isEqualTo(text);
        assertThat(after.getWordCount()).isEqualTo(cached.getWordCount());
    }

    private RisNormResult resultFor(RisNormMetadaten metadaten, NormDocumentEntity existing) {
        return resultFor(metadaten, existing, LocalDate.now());
    }

    /** The change date is what tells a rewritten document from one that was only re-delivered. */
    private RisNormResult resultFor(RisNormMetadaten metadaten, NormDocumentEntity existing,
                                    LocalDate changed) {
        RisMetadaten technical = new RisMetadaten(existing.getDocNumber(), null, null, null,
                changed, existing.getHtmlUrl(), "{}");
        return new RisNormResult(technical, metadaten, existing.getHtmlUrl(), List.of());
    }
}
