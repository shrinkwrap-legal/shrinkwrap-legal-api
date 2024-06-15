package legal.shrinkwrap.api.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Objects;

/**
 * TODO
 * - austrian academic grades
 * - austrian dates in long form (Jänner instead of Januar)
 */
public class CoreNlpServiceImpl implements CoreNlpService {


    private final StanfordCoreNLP pipeline;

    public CoreNlpServiceImpl() {
        pipeline = new StanfordCoreNLP("german");
    }


    @Override
    public List<String> extractSentences(String text) {
        Objects.requireNonNull(text);

        Annotation document = new Annotation(text);

        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        return sentences.stream().map(Object::toString).toList();

    }

    private void fullAnnotation(List<CoreMap> sentences) {
        for (CoreMap sentence : sentences) {
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

        }
    }
}
