package legal.shrinkwrap.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("fast")
@Tag("release")
@SpringBootTest
class ApplicationTests extends SpringTest {

    @Test
    void contextLoads() {
    }

}
