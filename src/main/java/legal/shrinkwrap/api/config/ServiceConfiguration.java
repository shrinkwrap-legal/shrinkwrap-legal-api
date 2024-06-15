package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.service.CoreNlpService;
import legal.shrinkwrap.api.service.CoreNlpServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

    @Bean
    public HtmlDownloadService htmlDownloadService() {
        return new HtmlDownloadService();
    }

    @Bean
    public CoreNlpService coreNlpService() {
        return new CoreNlpServiceImpl();
    }
}
