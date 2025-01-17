package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import legal.shrinkwrap.api.config.AdapterConfiguration;
import legal.shrinkwrap.api.config.ChatClientConfiguration;
import legal.shrinkwrap.api.config.ServiceConfiguration;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class CaselawAnalyzerServiceTest {

    @Autowired
    private CaselawAnalyzerService caselawAnalyzerService;

    @Test
    void analyzeCaselaw() {
        Path directoryPath = Paths.get("c:\\tmp\\shrinkwrap");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.html")) {
            for (Path entry : stream) {
                System.out.println("Reading file: " + entry.getFileName());
                String content = new String(Files.readAllBytes(entry), StandardCharsets.UTF_8);
                CaseLawDataset ds = new CaseLawDataset(null,null,null,null,null,null,null,null,null,null,null,null,content,null);
                caselawAnalyzerService.analyzeCaselaw(ds);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}