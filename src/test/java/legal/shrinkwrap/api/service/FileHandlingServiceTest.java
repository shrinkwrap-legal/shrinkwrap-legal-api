package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.config.AdapterConfiguration;
import legal.shrinkwrap.api.config.CommonServiceConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AdapterConfiguration.class, CommonServiceConfiguration.class})
@TestPropertySource("classpath:application.properties")
class FileHandlingServiceTest {

    @Autowired
    private FileHandlingService fileHandlingService;

    @Test
    void testNonExistingFile() {
        String ecli = "ECLI:UI:OGH0002:2025:" + (new Date()).getTime() + ".24I.0113.000";
        String s = fileHandlingService.loadFile(ecli,"no-exist.html");
        assertNull(s);
    }

    @Test
    void saveAndLoadFile() {
        String content = "asdf";
        String ecli = "ECLI:UI:OGH0002:2025:" + (new Date()).getTime() + ".24I.0113.000";
        fileHandlingService.saveFile(ecli,"unittest.html",content);

        String s = fileHandlingService.loadFile(ecli, "unittest.html");
        assertEquals(content,s);
    }
}