package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Template;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.ResourceUtils;
import com.github.jknack.handlebars.Handlebars;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;


public class CaselawAnalyzerService {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Template partsTemplate;

    private final ChatClient chatClient;

    public CaselawAnalyzerService(ChatClient.Builder chatClientBuilder) {
        chatClient = chatClientBuilder.build();

        Handlebars handlebars = new Handlebars();
        try {
            String s = Files.readString(ResourceUtils.getFile("classpath:prompts/parts.hbs").toPath());
            partsTemplate = handlebars.compileInline(s);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public void analyzeCaselaw(CaseLawDataset caselaw) {
        try {
            List<String> sentences = null;
            if (Strings.isNotEmpty(caselaw.sentences())) {

                sentences = Arrays.asList(caselaw.sentences().split("\r\n"));
            }  else {
                String textFromHtml = null;
                textFromHtml = getTextFromHtml(caselaw.contentHtml());
                getSentencesFromCaseLaw(textFromHtml);
            }

            //build model
            List<SentenceModel> sentenceModels = new ArrayList<>();
            for (int i=0;i<sentences.size();i++) {
                SentenceModel s = new SentenceModel((i+1), sentences.get(i));
                sentenceModels.add(s);
            }
            SentencesModel model = new SentencesModel(sentenceModels);

            //apply template
            String aiQuery = partsTemplate.apply(model);

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
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    public String getTextFromHtml(String html) throws IOException, URISyntaxException, InterruptedException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("contentHtml", html);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);


        //get sentences
        // Create the POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8090/htmlToText"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        // Send the request and retrieve the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Return the response body
        String body = response.body();
        Map<String, String> result = objectMapper.readValue(body, Map.class);
        return result.get("text");
    }

    public List<String> getSentencesFromCaseLaw(String text) throws IOException, URISyntaxException, InterruptedException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("contentText", text);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);


        //get sentences
        // Create the POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8090/sentencier"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        // Send the request and retrieve the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Return the response body
        String body = response.body();
        Map result = objectMapper.readValue(body, Map.class);
        return (List<String>) result.get("sentences");
    }

    private static final record SentencesModel(List<SentenceModel> sentences) {
    }

    private static final record SentenceModel(int id, String sentence) {
    }
}
