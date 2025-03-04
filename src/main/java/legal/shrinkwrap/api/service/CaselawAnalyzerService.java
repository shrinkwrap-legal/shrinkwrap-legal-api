package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import com.github.jknack.handlebars.Handlebars;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class CaselawAnalyzerService {

    private final Map<String, Template> templates = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

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



    public void summarizeCaselaw(CaseLawDataset caselaw) {
        if (caselaw.sentences().isEmpty()) {
            throw new NotImplementedException();
        }
        String text = caselaw.sentences();
        TextModel model = new TextModel(text);

        try {
            String system = templates.get("summary.system").apply(model);
            String user = templates.get("summary").apply(model);

            Message systemMessage = new SystemMessage(system);
            Message userMessage = new UserMessage(user);
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model("gpt-4o-mini").build();
            Prompt p = new Prompt(List.of(systemMessage, userMessage), options);
            ChatResponse chatResponse = chatClient.prompt(p).call().chatResponse();
            String aireturn  = chatResponse.getResult().getOutput().getText();

            //sometimes, openai will start with "```"
            String cleanedAi = aireturn.replaceAll("```json", "").replaceAll("```", "");
            Map jsonReturn = objectMapper.readValue(cleanedAi, Map.class);

            System.out.println(aireturn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

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

    private static final record TextModel(String text) {}
}
