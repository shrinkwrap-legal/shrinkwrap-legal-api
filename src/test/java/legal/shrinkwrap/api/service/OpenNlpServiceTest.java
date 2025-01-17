package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.SpringTest;
import legal.shrinkwrap.api.config.TestServicesConfiguration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@ContextConfiguration(classes = TestServicesConfiguration.class)
public class OpenNlpServiceTest extends SpringTest {

    @Autowired
    @Qualifier("openNlpService")
    private NLPService openNlpService;

    String text = """
        Die DSB forderte die Beschuldigte daher mit Schreiben vom 16.05.2023 zu einer ergänzenden Stellungnahme, zur Klarstellung der Vollmachtbekanntgabe sowie Bekanntgabe ihres Jahresumsatzes auf. Die Beschuldigte verweigerte die weitere Mitwirkung im Verwaltungsstrafverfahren und erstattete keine weitere Stellungnahme in Reaktion darauf.
        
        Das Oberlandesgericht Wien hat als Berufungsgericht in der Strafsache gegen A und andere Angeklagte wegen des Verbrechens des Suchtgifthandels nach § 28a Abs 1 erster Fall, Abs 2 Z 2, Abs 4 Z 3 SMG und anderer strafbarer Handlungen über die Berufung des Angeklagten B gegen das Urteil des Landesgerichts Eisenstadt als Schöffengericht vom 23. August 2023, AZ 51 Hv 33/23b-147, nach der unter dem Vorsitz der Senatspräsidentin Mag. Seidl, im Beisein der Richterin Dr. Vetter und des Richters Dr. Farkas als weitere Senatsmitglieder in Gegenwart der Oberstaatsanwältin Mag. Strnad, des Angeklagten B* und seines Verteidigers Mag. Michael Slany durchgeführten öffentlichen  Berufungsverhandlung am 31. Jänner 2024 zu Recht erkannt: Der Berufung wird nicht Folge gegeben. Gemäß § 390a Abs 1 StPO fallen dem Angeklagten auch die Kosten des Rechtsmittelverfahrens zur Last.
        """;

    @Test
    public void testOpenNlpService() {

        List<String> sentences = openNlpService.extractSentences(text);
        assertThat(sentences).hasSize(9);

    }
}
