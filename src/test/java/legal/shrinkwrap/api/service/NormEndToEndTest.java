package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.SpringTest;
import legal.shrinkwrap.api.persistence.entity.NormDocumentEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole way through, against live RIS: import a law, find it by the name a person would
 * type, narrow it to a range of provisions on a given day, and read the text of one.
 * <p>
 * The Buchpreisbindungsgesetz 2023 is the fixture because it is small - 14 documents, one
 * version each - and because RIS itself barely finds it, which is what makes the name lookup
 * worth testing.
 */
@Tag("network")
@SpringBootTest
public class NormEndToEndTest extends SpringTest {

    /** Buchpreisbindungsgesetz 2023, abbreviation "BPrBG 2023". */
    private static final String BPRBG = "20012101";

    private static final LocalDate IN_FORCE = LocalDate.of(2024, 1, 1);
    private static final LocalDate BEFORE_IT_EXISTED = LocalDate.of(2020, 1, 1);

    @Autowired
    private NormImporter normImporter;

    @Autowired
    private NormService normService;

    @Test
    public void importThenFindThenRead() {
        NormEntity norm = normImporter.importLaw(null, BPRBG);

        assertThat(norm).isNotNull();
        assertThat(norm.getKurztitel()).startsWith("Buchpreisbindungsgesetz");
        assertThat(norm.getJurisdiction()).isEqualTo("AT");

        //compared by id: the lookup returns its own instance and the entity has no equals()
        //RIS writes the short title with a non-breaking space before the year, so without
        //normalising that away the law would not be findable by its plain name
        for (String query : List.of("buchpreisbindungsgesetz", "Buchpreisbindungsgesetz 2023",
                "BPrBG", "BPrBG 2023", "bprbg2023")) {
            assertThat(normService.findLaws(query, "AT"))
                    .as("lookup by %s", query)
                    .extracting(NormEntity::getId)
                    .contains(norm.getId());
        }

        List<NormDocumentEntity> inForce = normService.findProvisions(norm, IN_FORCE);
        assertThat(inForce).isNotEmpty();

        //the law only came into force in 2023
        assertThat(normService.findProvisions(norm, BEFORE_IT_EXISTED)).isEmpty();

        //"1a" sits after § 1, so the range starts at § 2 - a letter is a following provision,
        //not a variant of the one before it
        List<NormDocumentEntity> range = normService.findProvisions(norm, IN_FORCE, "1a", "3", null);
        assertThat(range).extracting(NormDocumentEntity::getArtikelParagraphAnlage)
                .containsExactly("§ 2", "§ 3");

        assertThat(normService.findProvisions(norm, IN_FORCE, "1", "3", null))
                .extracting(NormDocumentEntity::getArtikelParagraphAnlage)
                .containsExactly("§ 1", "§ 2", "§ 3");

        //the head document RIS numbers "§ 0" must not slip into a range that starts at 1
        assertThat(range).noneMatch(d -> "§ 0".equals(d.getArtikelParagraphAnlage()));

        //metadata is mirrored completely, text only on demand
        NormDocumentEntity first = range.getFirst();
        assertThat(first.getFullText()).isNull();

        NormDocumentEntity withText = normService.withText(first);
        assertThat(withText.getFullText())
                .contains("§ 1.")
                .contains("Schutz von Büchern als Kulturgut");
        assertThat(withText.getWordCount()).isGreaterThan(20L);
        assertThat(withText.getTextConversionVersion()).isEqualTo(NormService.TEXT_CONVERSION_VERSION);

        //the symbol belongs on the same line as the text it introduces, as RIS renders it
        assertThat(withText.getFullText()).doesNotContain("§ 1.\n");
    }
}
