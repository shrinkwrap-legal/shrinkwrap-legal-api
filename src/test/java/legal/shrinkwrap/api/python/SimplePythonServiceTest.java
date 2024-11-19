package legal.shrinkwrap.api.python;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SimplePythonServiceTest {


    @Autowired
    private SimplePythonService simplePythonService;

    @Test
    void sayHello() {
        simplePythonService.sayHello();
    }
}