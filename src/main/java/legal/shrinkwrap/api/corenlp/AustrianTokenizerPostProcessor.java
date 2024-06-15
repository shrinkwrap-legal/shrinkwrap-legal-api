package legal.shrinkwrap.api.corenlp;

import edu.stanford.nlp.international.german.process.GermanTokenizerPostProcessor;


/**
 * TODO pull request for german tokenizer
 *
 */
public class AustrianTokenizerPostProcessor extends GermanTokenizerPostProcessor {


    public AustrianTokenizerPostProcessor() {
        this.ordinalPredictingWords.add("Jänner");
        this.ordinalPredictingWords.add("Feber");
    }
}
