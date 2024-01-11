package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.dto.enums.OgdApplikationEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RisAdapterTest {

    @Autowired
    public RisAdapter adapter;
    @Test
    public void testGetVersion() {

        String version = adapter.getVersion();
        assertThat(version).isNotNull().isEqualTo("2.6");
    }


    @Test
    public void testGetJustiz() {

        var result = adapter.getJustiz(OgdApplikationEnum.Justiz, "RS0125227");
        assertThat(result).isNotNull();
    }



}
