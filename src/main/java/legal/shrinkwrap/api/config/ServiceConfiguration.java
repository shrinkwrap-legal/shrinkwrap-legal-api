package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.python.ShrinkwrapPythonRestService;
import legal.shrinkwrap.api.service.CommonSentenceService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import legal.shrinkwrap.api.service.CaselawAnalyzerService;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Import({CommonServiceConfiguration.class})
public class ServiceConfiguration {

    @Autowired
    @Qualifier("openAiChatClientBuilder")
    ChatClient.Builder chatClientBuilder;

    @Autowired
    ShrinkwrapPythonRestService shrinkwrapPythonRestService;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    CommonSentenceService commonSentenceService;

    @Bean
    public CaselawAnalyzerService caselawAnalyzerService() {
        return new CaselawAnalyzerService(chatClientBuilder, resourceLoader, commonSentenceService);
    }
}
