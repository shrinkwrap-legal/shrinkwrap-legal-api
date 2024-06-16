package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.dto.NlpTokenInfoDto;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class OpenNlpServiceImpl implements NLPService {

    private SentenceDetectorME sentenceDetector;

    public OpenNlpServiceImpl() {
        try {
            File file = ResourceUtils.getFile("classpath:nlp/opennlp/opennlp-de-ud-gsd-sentence-1.0-1.9.3.bin");
            SentenceModel model = new SentenceModel(file);
            sentenceDetector = new SentenceDetectorME(model);
        } catch (IOException e) {
        }

    }


    @Override
    public List<String> extractSentences(String text) {
        String[] sentences = sentenceDetector.sentDetect(text);
        return Arrays.asList(sentences);
    }

    @Override
    public List<NlpTokenInfoDto> extractTokens(String text) {
        return List.of();
    }
}
