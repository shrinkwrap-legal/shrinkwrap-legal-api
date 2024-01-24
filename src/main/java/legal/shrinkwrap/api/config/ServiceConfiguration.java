package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

    @Bean
    public HtmlDownloadService htmlDownloadService() {
        return new HtmlDownloadService();
    }
}
