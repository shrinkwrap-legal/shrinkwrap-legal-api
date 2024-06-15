package legal.shrinkwrap.api.service;


import legal.shrinkwrap.api.SpringTest;
import legal.shrinkwrap.api.config.TestServicesConfiguration;
import legal.shrinkwrap.api.dto.NlpTokenInfoDto;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test configuration of stanford NLP pipeline
 * https://dkpro.github.io/dkpro-core/
 *
 */
@ContextConfiguration(classes = TestServicesConfiguration.class)
public class CoreNlpServiceTest extends SpringTest {

    @Autowired
    private CoreNlpService coreNlpService;

    String text = """
        Die DSB forderte die Beschuldigte daher mit Schreiben vom 16.05.2023 zu einer ergänzenden Stellungnahme, zur Klarstellung der Vollmachtbekanntgabe sowie Bekanntgabe ihres Jahresumsatzes auf. Die Beschuldigte verweigerte die weitere Mitwirkung im Verwaltungsstrafverfahren und erstattete keine weitere Stellungnahme in Reaktion darauf.
        
        Das Oberlandesgericht Wien hat als Berufungsgericht in der Strafsache gegen A und andere Angeklagte wegen des Verbrechens des Suchtgifthandels nach § 28a Abs 1 erster Fall, Abs 2 Z 2, Abs 4 Z 3 SMG und anderer strafbarer Handlungen über die Berufung des Angeklagten B gegen das Urteil des Landesgerichts Eisenstadt als Schöffengericht vom 23. August 2023, AZ 51 Hv 33/23b-147, nach der unter dem Vorsitz der Senatspräsidentin Mag. Seidl, im Beisein der Richterin Dr. Vetter und des Richters Dr. Farkas als weitere Senatsmitglieder in Gegenwart der Oberstaatsanwältin Mag. Strnad, des Angeklagten B* und seines Verteidigers Mag. Michael Slany durchgeführten öffentlichen  Berufungsverhandlung am 31. Jänner 2024 zu Recht erkannt: Der Berufung wird nicht Folge gegeben. Gemäß § 390a Abs 1 StPO fallen dem Angeklagten auch die Kosten des Rechtsmittelverfahrens zur Last.
        """;

    @Test
    public void test() {

        List<String> sentences = coreNlpService.extractSentences(text);

        // sentences.forEach(System.out::println);

        assertThat(sentences).hasSize(7);

    }

    @Test
    public void test_withHtmlFile() throws IOException {
        File file = ResourceUtils.getFile("classpath:files/ris/DSBT_20231207_2023_0_583_644_00.html");

        assertThat(file).isNotNull();

        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        assertThat(text).isNotEmpty();
    }


    @Test
    public void test_withTokens() {

        List<NlpTokenInfoDto> tokenInfos =  coreNlpService.extractTokens(text);

        assertThat(tokenInfos).isNotEmpty().hasSize(197);

        tokenInfos.stream().forEach(System.out::println);

    }

}
