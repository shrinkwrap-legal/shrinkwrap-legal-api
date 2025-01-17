package legal.shrinkwrap.api.adapter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("network")
@SpringBootTest
public class HtmlDownloadServiceTest {


    @Autowired
    private HtmlDownloadService htmlDownloadService;


    @Test
    public void testDownloadHtml() {
        String response = htmlDownloadService.downloadHtml("https://www.ris.bka.gv.at/Dokumente/Justiz/JJR_20090723_OGH0002_0130NS00046_09G0000_001/JJR_20090723_OGH0002_0130NS00046_09G0000_001.html");
        assertThat(response).isNotNull();
    }
}
