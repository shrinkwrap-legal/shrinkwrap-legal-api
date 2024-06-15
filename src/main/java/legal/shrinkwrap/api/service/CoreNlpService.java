package legal.shrinkwrap.api.service;

import java.util.List;

/**
 * https://www.baeldung.com/java-nlp-libraries
 */
public interface CoreNlpService {

    /**
     *
     * @param text
     * @return
     */
    List<String> extractSentences(String text);
}
