package legal.shrinkwrap.api.data;


import legal.shrinkwrap.api.adapter.ris.RisSoapAdapterImpl;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.config.AdapterConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AdapterConfiguration.class)
public class FetchJudikaturTest {

    @Autowired
    private RisSoapAdapterImpl risSoapAdapter;


    @Test
    public void test_getJustiz() {
        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(RisCourt.Justiz, null);
        assertThat(result).isNotNull();
        assertThat(result.getJudikaturResults()).isNotNull().hasSize(20);
        System.out.println(result);
    }
}
