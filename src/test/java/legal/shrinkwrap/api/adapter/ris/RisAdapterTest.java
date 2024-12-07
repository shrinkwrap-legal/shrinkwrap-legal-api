package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.rest.dto.OgdDocumentReference;
import legal.shrinkwrap.api.adapter.ris.rest.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.rest.dto.enums.OgdApplikationEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

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

        Optional<OgdDocumentReference.Dokumentliste.ContentReference.DokumentlisteUrl.ContentUrl> contentUrl = result.ogdDocumentResults().documentList().getFirst().getData().getDokumentliste().getContentReference().getUrls().getContentUrls().stream().filter(url -> url.getDataType().equals("Html")).findFirst();



    }



}
