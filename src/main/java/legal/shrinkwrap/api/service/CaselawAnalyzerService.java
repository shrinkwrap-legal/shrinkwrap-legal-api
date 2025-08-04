package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.jakarta.types.ObjectSchema;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.utils.SentenceHashingTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import com.github.jknack.handlebars.Handlebars;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class CaselawAnalyzerService {
    private final Integer MAX_TOKEN = 100000;
    private final Integer TOKEN_SYSTEM_AND_PROMPT_ESTIMATION;
    private final String AI_MODEL = "gpt-4o-mini";
    private final Map<String, Template> templates = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();
    JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);

    private final ChatClient chatClient;

    private final CommonSentenceService commonSentenceService;


    public CaselawAnalyzerService(ChatClient.Builder chatClientBuilder, ResourceLoader resourceLoader, CommonSentenceService commonSentenceService) {
        chatClient = chatClientBuilder.build();
        this.commonSentenceService = commonSentenceService;

        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper("germanNumber", (Object numberStr, Options options) -> {
            Integer number = Integer.parseInt(numberStr.toString());
            return switch (number) {
                case 1 -> "einen";
                case 2 -> "zwei";
                case 3 -> "drei";
                case 4 -> "vier";
                case 5 -> "fünf";
                default -> null;
            };
        });
        try {
            Resource resource = resourceLoader.getResource("classpath:prompts/parts.hbs");
            String s = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
            Template template = handlebars.compileInline(s);
            templates.put("parts", template);

            resource = resourceLoader.getResource("classpath:prompts/summary.hbs");
            s = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
            template = handlebars.compileInline(s);
            templates.put("summary", template);

            resource = resourceLoader.getResource("classpath:prompts/summary.system.hbs");
            s = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
            template = handlebars.compileInline(s);
            templates.put("summary.system", template);

            //token estimation for system/user
            TextModel dummyModel = new TextModel("",true, false, 3, true, false);
            String system = templates.get("summary.system").apply(dummyModel);
            String user = templates.get("summary").apply(dummyModel);
            TOKEN_SYSTEM_AND_PROMPT_ESTIMATION = tokenCountEstimator.estimate(system + " " + user);
            log.info("loaded summary prompts, " + TOKEN_SYSTEM_AND_PROMPT_ESTIMATION + " token(s)");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public SummaryAnalysis summarizeCaselaw(String text) {
        return summarizeCaselaw(text, null);
    }

    public SummaryAnalysis summarizeCaselaw(String text, CaseLawEntity entity) {
        boolean isCriminal = entity != null && StringUtils.defaultString(entity.getCaseNumber()).matches("^[\\d]+Os.*");
        boolean isVfGH = entity != null && entity.getApplicationType().equalsIgnoreCase(RisCourt.VfGH.toString());
        boolean isVwGH = entity != null && entity.getApplicationType().equalsIgnoreCase(RisCourt.VwGH.toString());
        boolean isPart = false;
        int wordCount = text.split(" ").length;
        int numberOfSentences = getNumberOfSentencesSuitableByWordCount(wordCount);
        int tokenEstimation = TOKEN_SYSTEM_AND_PROMPT_ESTIMATION + tokenCountEstimator.estimate(text);
        String removedText = null;
        if (tokenEstimation > (MAX_TOKEN *2)) {
            log.info("skipping summary " + (entity != null ? entity.getDocNumber() : "unknown ") + ", approx " + tokenEstimation + " token");
            return null;
        }
        else if (tokenEstimation > MAX_TOKEN) {
            int overhead  = tokenEstimation - MAX_TOKEN;
            int textLength = text.length();
            int cuttingCenter = textLength / 2;
            //try to cut some text from the middle
            for (float charsPerToken = 1.6f;charsPerToken < 5f; charsPerToken += 0.1f) {
                int charsToCut = Math.round(overhead * charsPerToken);
                String textToCut = text.substring(cuttingCenter - charsToCut/2, cuttingCenter + charsToCut/2);
                int cutToken = tokenCountEstimator.estimate(textToCut);
                if (cutToken > overhead) {
                    text = text.substring(0, cuttingCenter - charsToCut / 2) + "...\n\n (...) \n\n..." + text.substring(cuttingCenter + charsToCut / 2);
                    log.info("cut " + cutToken + " token(s) at " + Math.round(charsPerToken * 10) / 10f + " char per token");
                    removedText = textToCut;
                    break;
                }
            }
            isPart = true;
            tokenEstimation = TOKEN_SYSTEM_AND_PROMPT_ESTIMATION + tokenCountEstimator.estimate(text);
        }

        TextModel model = new TextModel(text, isCriminal, isVfGH, numberOfSentences, isPart, isVwGH);

        try {
            String system = templates.get("summary.system").apply(model);
            String user = templates.get("summary").apply(model);
            user = user.replaceAll("\n\n","\n").replaceAll("\r\n\r\n","\r\n");

            //try generating json schema
            ObjectSchema jsonSchema = schemaGen.generateSchema(CaselawSummaryCivilCase.class).asObjectSchema();
            jsonSchema.setAdditionalProperties(ObjectSchema.NoAdditionalProperties.instance);
            String schema = objectMapper.writeValueAsString(jsonSchema);
            ResponseFormat format = new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, schema);
            Message systemMessage = new SystemMessage(system);
            Message userMessage = new UserMessage(user);
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(AI_MODEL).build();
            Prompt p = new Prompt(List.of(systemMessage, userMessage), options);
            log.info("requesting summary " + (entity != null ? entity.getDocNumber() : "unknown ") + ", approx " + tokenEstimation + " token");

            for (int j = 0; j < 2; j++) {
                ChatResponse chatResponse = chatClient.prompt(p).call().chatResponse();
                String aireturn = chatResponse.getResult().getOutput().getText();

                //sometimes, openai will start with "```"
                String cleanedAi = aireturn.replaceAll("```json", "").replaceAll("```", "");
                CaselawSummaryCivilCase jsonReturn = null;
                try {
                    jsonReturn = objectMapper.readValue(cleanedAi, CaselawSummaryCivilCase.class);

                    SummaryAnalysis sa = new SummaryAnalysis(jsonReturn, system, user, removedText, AI_MODEL);
                    return sa;
                } catch (JsonProcessingException e) {
                    if (j == 0) {
                        log.error("could not match, retry");
                    } else {
                        log.error("could not match on retry");
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Pair<String, List<String>> shortenTextBasedOnCommonSentences(String fullText, CaseLawEntity entity) {
        List<SentenceHashingTools.HashedSentence> model = SentenceHashingTools.getSentenceModel(fullText);
        String sentenceHash = SentenceHashingTools.getHashFromModel(model);

        //Find common sentences
        List<String> containedSentences = commonSentenceService.findContainedSentences(sentenceHash);

        List<List<SentenceHashingTools.HashedSentence>> sentencesToReplace = new ArrayList<>();
        List<String> replacedSentences = new ArrayList<>();
        int replacedTokens = 0;
        for (int i = 0; i < containedSentences.size(); i++) {
            String commonHash = containedSentences.get(i);
            int startPos = sentenceHash.indexOf(commonHash);
            int endPos = startPos + commonHash.length();

            SentenceHashingTools.HashedSentence sentence1 = model.get(startPos);
            SentenceHashingTools.HashedSentence sentence2 = model.get(endPos-1);
            sentencesToReplace.add(List.of(sentence1, sentence2));
        }

        //combine overlapping segments (which may happen)
        SentenceHashingTools.combineOverlapping(sentencesToReplace);

        //sort by approx sentence length
        sentencesToReplace.sort(Comparator.comparingInt(i -> ((i.getLast().getEndPos()) - i.getFirst().getEndPos())));
        Collections.reverse(sentencesToReplace);

        //remove all (@TODO: Check if it would be better to remove only until token count is reached.
        //for now, the suspicion is that all this text is irrelevant, so it should be removed.
        for (int i = 0; i < sentencesToReplace.size(); i++) {
            String commonSentence = SentenceHashingTools.getCommonSentence(fullText, sentencesToReplace.get(i), model);
            replacedSentences.add(commonSentence);
            replacedTokens += tokenCountEstimator.estimate(commonSentence);
        }

        //replace all
        String textWithReplacements = SentenceHashingTools.replaceCommonSentence(fullText, sentencesToReplace);
        return Pair.of(textWithReplacements, replacedSentences);
    }

    @Deprecated
    public void analyzeCaselaw(CaseLawDataset caselaw) {
        try {
            List<String> sentences = null;
            if (Strings.isNotEmpty(caselaw.sentences())) {

                sentences = Arrays.asList(caselaw.sentences().split("\n"));
            }  else {
                String textFromHtml = null;
                throw new NotImplementedException();
            }

            //build model
            List<SentenceModel> sentenceModels = new ArrayList<>();
            for (int i=0;i<sentences.size();i++) {
                SentenceModel s = new SentenceModel((i+1), sentences.get(i));
                sentenceModels.add(s);
            }
            SentencesModel model = new SentencesModel(sentenceModels);

            //apply template
            String aiQuery = templates.get("parts").apply(model);

            //get from AI
            //@TODO: get token usage, maybe split

            Prompt p = new Prompt(aiQuery);
            ChatResponse chatResponse = chatClient.prompt(p).call().chatResponse();
            String text = chatResponse.getResult().getOutput().getText();

            //from text, generate debug html
            //split
            List<String> linesFromOpenAi = Arrays.asList(text.split("\n"));

            //for demo, output all to html

            String innerHtml = "";
            for (int i = 0; i < sentences.size(); i++) {
                innerHtml += "<p class='" + linesFromOpenAi.get(i).replace(":"," ").replace(",", " ") + "'>" + sentences.get(i) + "</p>";
            }
            String css = """
                    <style>
                    .a {
                    background-color: lime;
                    }
                    .b {
                    background-color: salmon;
                    }
                    .c {
                    background-color: gray;
                    }
                    .d {
                    background-color: cyan;
                    }
                    .e {
                    background-color: yellow;
                    }
                    .f {
                    background-color: orange;
                    }
                    .g {
                    background-color: pink;
                    }
                    .h {
                    background-color: brown;
                    }
                    .i {
                    background-color: darkred;
                    }
                    </style>
                    """;
            String fullHtml = "<html><head>" + css + "</head><body>" + innerHtml + "</body></html>";
            System.out.println(fullHtml);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }



    private static final record SentencesModel(List<SentenceModel> sentences) {
    }

    private static final record SentenceModel(int id, String sentence) {
    }

    private static final record TextModel(String text, Boolean criminal, Boolean VfGH, int numberOfSentences, boolean isPart, boolean VwGH) {}

    private int getNumberOfSentencesSuitableByWordCount(int wordCount) {
        if (wordCount < 200) {
            return 1;
        }
        if (wordCount < 700) {
            return 2;
        }
        if (wordCount < 4000) {
            return 3;
        }
        if (wordCount < 8000) {
            return 4;
        }
        return 5;
    }

    public static final record SummaryAnalysis(CaselawSummaryCivilCase summary, String systemPrompt, String userPrompt, String removedFromPrompt, String model) {};
}
