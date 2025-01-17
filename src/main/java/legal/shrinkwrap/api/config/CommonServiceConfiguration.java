package legal.shrinkwrap.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;

@Configuration
public class CommonServiceConfiguration {

    @Bean
    public HtmlDownloadService htmlDownloadService() {
        return new HtmlDownloadService();
    }

}
