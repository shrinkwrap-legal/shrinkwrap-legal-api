package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.service.CaselawAnalyzerService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

    @Autowired
    @Qualifier("openAiChatClientBuilder")
    ChatClient.Builder chatClientBuilder;

    @Bean
    public HtmlDownloadService htmlDownloadService() {
        return new HtmlDownloadService();
    }

    @Bean
    public CaselawAnalyzerService caselawAnalyzerService() {
        return new CaselawAnalyzerService(chatClientBuilder);
    }
}
