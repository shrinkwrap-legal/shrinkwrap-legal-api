package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.python.ShrinkwrapPythonRestService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import legal.shrinkwrap.api.service.CaselawAnalyzerService;

@Configuration
@Import({CommonServiceConfiguration.class})
public class ServiceConfiguration {

    @Autowired
    @Qualifier("openAiChatClientBuilder")
    ChatClient.Builder chatClientBuilder;

    @Autowired
    ShrinkwrapPythonRestService shrinkwrapPythonRestService;

    @Bean
    public CaselawAnalyzerService caselawAnalyzerService() {
        return new CaselawAnalyzerService(chatClientBuilder, shrinkwrapPythonRestService);
    }
}
