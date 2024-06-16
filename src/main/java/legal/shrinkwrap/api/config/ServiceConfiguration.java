package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.service.NLPService;
import legal.shrinkwrap.api.service.CoreNlpServiceImpl;
import legal.shrinkwrap.api.service.OpenNlpServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

    @Bean
    public HtmlDownloadService htmlDownloadService() {
        return new HtmlDownloadService();
    }

    @Bean("coreNlpService")
    public NLPService coreNlpService() {
        return new CoreNlpServiceImpl();
    }

    @Bean("openNlpService")
    public NLPService openNLPService() {
        return new OpenNlpServiceImpl();
    }
}
