package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.jakarta.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.jakarta.types.ObjectSchema;
import com.github.jknack.handlebars.Template;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import com.github.jknack.handlebars.Handlebars;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class CaselawAnalyzerService {

    private final Map<String, Template> templates = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);

    private final ChatClient chatClient;


    public CaselawAnalyzerService(ChatClient.Builder chatClientBuilder, ResourceLoader resourceLoader) {
        chatClient = chatClientBuilder.build();

        Handlebars handlebars = new Handlebars();
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

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public CaselawSummaryCivilCase summarizeCaselaw(String text) {
        return summarizeCaselaw(text, null);
    }

    public CaselawSummaryCivilCase summarizeCaselaw(String text, CaseLawEntity entity) {
        boolean isCriminal = entity != null && StringUtils.defaultString(entity.getCaseNumber()).matches("[\\d]+Os.*");
        TextModel model = new TextModel(text, isCriminal);

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
                    .model("gpt-4o-mini").build();
            Prompt p = new Prompt(List.of(systemMessage, userMessage), options);
            ChatResponse chatResponse = chatClient.prompt(p).call().chatResponse();
            String aireturn  = chatResponse.getResult().getOutput().getText();

            //sometimes, openai will start with "```"
            String cleanedAi = aireturn.replaceAll("```json", "").replaceAll("```", "");
            CaselawSummaryCivilCase jsonReturn = null;
            try {
                jsonReturn = objectMapper.readValue(cleanedAi, CaselawSummaryCivilCase.class);
            } catch (JsonProcessingException e) {
                System.out.println("could not match");
                return null;
            }

            return jsonReturn;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

    private static final record TextModel(String text, Boolean criminal) {}
}
