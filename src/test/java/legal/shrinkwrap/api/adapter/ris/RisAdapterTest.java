package legal.shrinkwrap.api.adapter.ris;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RisAdapterTest {

    @Autowired
    public RisAdapter adapter;
    @Test
    public void testGetVersion() {

        String version = adapter.getVersion();
        assertThat(version).isNotNull().isEqualTo("2.6");
    }

}
