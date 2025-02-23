package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.python.ShrinkwrapPythonRestService;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.ResourceUtils;
import com.github.jknack.handlebars.Handlebars;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;


public class CaselawAnalyzerService {
    private final String outputdir = "/Users/thomas/Downloads/ECLI/";
    private final Map<String, Template> templates = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatClient chatClient;

    private final ShrinkwrapPythonRestService pythonRestService;

    public CaselawAnalyzerService(ChatClient.Builder chatClientBuilder, ShrinkwrapPythonRestService pythonRestService) {
        chatClient = chatClientBuilder.build();
        this.pythonRestService = pythonRestService;

        Handlebars handlebars = new Handlebars();
        try {
            String s = Files.readString(ResourceUtils.getFile("classpath:prompts/parts.hbs").toPath());
            Template template = handlebars.compileInline(s);
            templates.put("parts", template);

            s = Files.readString(ResourceUtils.getFile("classpath:prompts/summary.hbs").toPath());
            template = handlebars.compileInline(s);
            templates.put("summary", template);

            s = Files.readString(ResourceUtils.getFile("classpath:prompts/summary.system.hbs").toPath());
            template = handlebars.compileInline(s);
            templates.put("summary.system", template);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public void summarizeCaselaw(CaseLawAnalysisEntity caselaw) {
        if (caselaw.getFullText().isEmpty()) {
            throw new NotImplementedException();
        }
        String text = caselaw.getFullText();
        TextModel model = new TextModel(text);
        String ecli = caselaw.getCaseLaw().getEcli();

        try {
            String system = templates.get("summary.system").apply(model);
            String user = templates.get("summary").apply(model);

            /*
            Message systemMessage = new SystemMessage(system);
            Message userMessage = new UserMessage(user);
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model("gpt-4o-mini").build();
            Prompt p = new Prompt(List.of(systemMessage, userMessage), options);
            ChatResponse chatResponse = chatClient.prompt(p).call().chatResponse();
            String aireturn  = chatResponse.getResult().getOutput().getText();*/
            String aireturn = "";
            try {
                    String openAIBatchJson = toOpenAIBatchJson(user, system, ecli);
                writeToAppropriateFile(openAIBatchJson);
                return;
                //aireturn = callOllamaFromChristian(user);
            }
            catch (Exception e) {

            }


            //sometimes, openai will start with "```"
            String cleanedAi = aireturn.replaceAll("```json", "").replaceAll("```", "").trim();
            Map jsonReturn = objectMapper.readValue(cleanedAi, Map.class);

            System.out.println(aireturn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public String callOllamaFromChristian(String prompt) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        Map<String, Object> params = new HashMap<>();
        params.put("model", "qwen2.5:32b");
        params.put("stream", false);
        params.put("prompt",prompt);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(params);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://XXXXXXX:9999/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response status code: " + response.statusCode());

        Map<String,Object> map = objectMapper.readValue(response.body(), Map.class);
        return map.get("response").toString();
    }

    public String toOpenAIBatchJson(String system, String user, String ecli) throws JsonProcessingException {
        Map<String, Object> request = new HashMap<>();
        request.put("custom_id", ecli);
        request.put("method", "POST");
        request.put("url", "/v1/chat/completions");

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("max_tokens", 10000);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", system);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", user);
        messages.add(userMessage);

        body.put("messages", messages);
        request.put("body", body);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(request);
        //System.out.println(json);
        return json;
    }

    public void writeToAppropriateFile(String content) {
        try {
            int fileNumber = 0;
            String fileName = outputdir + "file.json";

            while (true) {
                File file = new File(fileName);
                if (!file.exists() || Files.size(Paths.get(fileName)) < 199 * 1024 * 1024) { // 199 MB in bytes) {
                    try (FileWriter writer = new FileWriter(file, true)) {
                        writer.write(content + '\n');
                    }
                    catch (IOException e) {}
                    break;
                } else {
                    fileNumber++;
                    fileName = outputdir + "file" + fileNumber + ".json";
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
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
