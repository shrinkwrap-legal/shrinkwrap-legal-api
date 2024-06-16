package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.dto.NlpTokenInfoDto;

import java.util.List;

/**
 * https://www.baeldung.com/java-nlp-libraries
 */
public interface NLPService {

    /**
     *
     * @param text
     * @return
     */
    List<String> extractSentences(String text);

    /**
     *
     * @param text
     * @return
     */
    List<NlpTokenInfoDto> extractTokens(String text);
}
