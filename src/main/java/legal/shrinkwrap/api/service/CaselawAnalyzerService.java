package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CaselawAnalyzerService {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public void analyzeCaselaw(CaseLawDataset caselaw) {
        String textFromHtml = null;
        try {
            textFromHtml = getTextFromHtml(caselaw.contentHtml());
            List<String> sentences = getSentencesFromCaseLaw(textFromHtml);
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
}
