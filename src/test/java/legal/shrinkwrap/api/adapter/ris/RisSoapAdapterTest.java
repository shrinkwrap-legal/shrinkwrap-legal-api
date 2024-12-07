package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLawBuilder;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.config.AdapterConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(RisSearchParameterCaseLawBuilder.builder()
                .court(RisCourt.Justiz)
                .ecli("ECLI:AT:OGH0002:2017:0140OS00062.17Z.1107.000").build());

        assertThat(result).isNotNull();
        assertThat(result.getJudikaturResults()).hasSize(1);
    }
}
