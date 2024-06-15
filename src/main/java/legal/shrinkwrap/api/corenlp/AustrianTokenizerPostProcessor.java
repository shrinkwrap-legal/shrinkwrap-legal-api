package legal.shrinkwrap.api.corenlp;

import edu.stanford.nlp.international.german.process.GermanTokenizerPostProcessor;

public class AustrianTokenizerPostProcessor extends GermanTokenizerPostProcessor {


    public AustrianTokenizerPostProcessor() {
        this.ordinalPredictingWords.add("Jänner"); // = new HashSet(Arrays.asList("Januar", "Jänner", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember", "Jahrhundert"));
    }
}
