package legal.shrinkwrap.api.data;


import at.gv.ris.api.v26.api.JudikaturRisApi;
import at.gv.ris.api.v26.model.OgdSearchResult;
import legal.shrinkwrap.api.adapter.ris.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.enums.OgdApplikationEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FetchJudikatur {

    @Autowired
    private JudikaturRisApi judikaturRisApi;

    @Test
    public void fetchJudikatur() {

        OgdSearchResult result = judikaturRisApi.postQueryJudikaturJustiz("Justiz", null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
    }

}
