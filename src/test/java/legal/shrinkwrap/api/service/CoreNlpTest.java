package legal.shrinkwrap.api.service;


import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test configuration of stanford NLP pipeline
 * https://dkpro.github.io/dkpro-core/
 *
 * TODO
 * - austrian academic grades
 * - austrian dates in long form (Jänner instead of Januar)
 */
public class CoreNlpTest {

    @Test
    public void test() throws IOException {

        File file = ResourceUtils.getFile("classpath:files/ris/DSBT_20231207_2023_0_583_644_00.html");

        assertThat(file).isNotNull();

        Properties props = new Properties();
        props.put("annotators", "tokenize,cleanxml, ssplit, pos");
        // props.put("annotators", "tokenize");
        StanfordCoreNLP pipeline = new StanfordCoreNLP("german");

        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        text = """
        Die DSB forderte die Beschuldigte daher mit Schreiben vom 16.05.2023 zu einer ergänzenden Stellungnahme, zur Klarstellung der Vollmachtbekanntgabe sowie Bekanntgabe ihres Jahresumsatzes auf. Die Beschuldigte verweigerte die weitere Mitwirkung im Verwaltungsstrafverfahren und erstattete keine weitere Stellungnahme in Reaktion darauf.
        
        Das Oberlandesgericht Wien hat als Berufungsgericht in der Strafsache gegen A und andere Angeklagte wegen des Verbrechens des Suchtgifthandels nach § 28a Abs 1 erster Fall, Abs 2 Z 2, Abs 4 Z 3 SMG und anderer strafbarer Handlungen über die Berufung des Angeklagten B gegen das Urteil des Landesgerichts Eisenstadt als Schöffengericht vom 23. August 2023, AZ 51 Hv 33/23b-147, nach der unter dem Vorsitz der Senatspräsidentin Mag. Seidl, im Beisein der Richterin Dr. Vetter und des Richters Dr. Farkas als weitere Senatsmitglieder in Gegenwart der Oberstaatsanwältin Mag. Strnad, des Angeklagten B* und seines Verteidigers Mag. Michael Slany durchgeführten öffentlichen  Berufungsverhandlung am 31. Januar 2024 zu Recht erkannt: Der Berufung wird nicht Folge gegeben. Gemäß § 390a Abs 1 StPO fallen dem Angeklagten auch die Kosten des Rechtsmittelverfahrens zur Last.
        """;

        assertThat(text).isNotNull();

        Annotation document = new Annotation(text);

        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for(CoreMap sentence: sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);


                // this is the NER label of the token
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                //System.out.println("word: " + word + " pos: " + pos + " ne:" + ne);
            }

            System.out.println("sentence: " + sentence);

        }



    }

}
