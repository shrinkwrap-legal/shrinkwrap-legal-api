package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.NormabschnittTyp;
import at.gv.bka.ris.v26.soap.ws.client.WebDocumentContentType;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.config.AdapterConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AdapterConfiguration.class)
public class RisSoapAdapterTest {

    @Autowired
    private RisSoapAdapterImpl risSoapAdapter;

    @Test
    public void test_getVersion() {
        String version = risSoapAdapter.getVersion();
        assertThat(version).isNotNull().isEqualTo("OGD_RIS V2_6");
    }

    @Test
    public void test_findDocumentForEuropeanCaseLawIdentifier() {
        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(RisSearchParameterCaseLaw.builder()
                .court(RisCourt.Justiz)
                .ecli("ECLI:AT:OGH0002:2017:0140OS00062.17Z.1107.000").build());

        assertThat(result).isNotNull();
        assertThat(result.getJudikaturResults()).hasSize(1);
    }

    @Test
    public void test_findDocumentForChanges() {
        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(RisSearchParameterCaseLaw.builder()
                .changedInLastXDays(5)
                .court(RisCourt.Justiz)
                .build());
        assertThat(result).isNotNull();
    }

    /** StVO Gesetzesnummer 10011336; § 50 exists in several versions. */
    private static final String STVO = "10011336";

    @Test
    public void test_findNormDocumentsForParagraph() {
        List<RisNormResult> results = risSoapAdapter.findNormDocuments(RisSearchParameterNorm.builder()
                .gesetzesnummer(STVO)
                .abschnitt(NormabschnittTyp.PARAGRAPH, "50", "50")
                .build());

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(result -> {
            assertThat(result.getNormMetadaten().getGesetzesnummer()).isEqualTo(STVO);
            assertThat(result.getNormMetadaten().getArtikelParagraphAnlage()).isEqualTo("§ 50");
            assertThat(result.getNormMetadaten().getAbkuerzung()).isEqualTo("StVO 1960");
            assertThat(result.getHtmlDocumentUrl()).endsWith(".html");
            assertThat(result.getMetadaten().getId()).startsWith("NOR");
        });
    }

    /**
     * § 50 StVO consists of road signs. They are attachments of the document, and the html only
     * references them relative - hence they have to arrive here with an absolute url.
     */
    @Test
    public void test_findNormDocumentsCarriesImagesAsAttachments() {
        List<RisNormResult> results = risSoapAdapter.findNormDocuments(RisSearchParameterNorm.builder()
                .gesetzesnummer(STVO)
                .abschnitt(NormabschnittTyp.PARAGRAPH, "50", "50")
                .build());

        RisNormResult withImages = results.stream()
                .filter(r -> !r.getAttachments().isEmpty())
                .findFirst().orElseThrow();

        assertThat(withImages.getAttachments()).allSatisfy(attachment -> {
            assertThat(attachment.kind()).isEqualTo(WebDocumentContentType.EMBEDDED_ATTACHMENT);
            assertThat(attachment.url()).startsWith("https://");
            assertThat(attachment.name()).isNotBlank();
        });
    }

    @Test
    public void test_findNormDocumentsForOneFassung() {
        List<RisNormResult> results = risSoapAdapter.findNormDocuments(RisSearchParameterNorm.builder()
                .gesetzesnummer(STVO)
                .abschnitt(NormabschnittTyp.PARAGRAPH, "50", "50")
                .fassungVom(LocalDate.of(2020, 1, 1))
                .build());

        //a single point in time can only ever have one version in force
        assertThat(results).hasSize(1);
    }

    /**
     * RIS drops an incomplete Abschnitt restriction without an error and answers with the whole
     * corpus, so the parameter object has to refuse it.
     */
    @Test
    public void test_incompleteAbschnittIsRejected() {
        assertThatThrownBy(() -> RisSearchParameterNorm.builder()
                .gesetzesnummer(STVO)
                .abschnitt(NormabschnittTyp.PARAGRAPH, null, null)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
