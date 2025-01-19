package legal.shrinkwrap.api.python;

import com.fasterxml.jackson.databind.ObjectMapper;
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

public class ShrinkwrapPythonRestService
{
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    /**
     * Get pandoc response for a given HTML
     * @param html
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public String getTextFromHtml(String html) throws IOException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("contentHtml", html);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);


        //get sentences
        // Create the POST request
        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
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
        } catch (URISyntaxException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    /**
     * Get sentences in array from given clear text
     * @param text
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     */
    public List<String> getSentencesFromCaseLaw(String text) throws IOException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("contentText", text);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);


        //get sentences
        // Create the POST request
        HttpRequest request = null;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8090/sentencier"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();
            // Send the request and retrieve the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Return the response body
            String body = response.body();
            Map result = objectMapper.readValue(body, Map.class);
            List<String> sentences = (List<String>) result.get("sentences");
            return sentences;
        } catch (URISyntaxException | InterruptedException e) {
            throw new IOException(e);
        }


    }
}
