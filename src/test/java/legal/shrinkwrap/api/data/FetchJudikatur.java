package legal.shrinkwrap.api.data;


import legal.shrinkwrap.api.adapter.ris.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.enums.OgdApplikationEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FetchJudikatur {

    @Autowired
    public RisAdapter adapter;

    @Test
    public void fetchJudikatur() {
        var result = adapter.getJustiz(OgdApplikationEnum.Justiz);
        assertThat(result).isNotNull();
    }

}
