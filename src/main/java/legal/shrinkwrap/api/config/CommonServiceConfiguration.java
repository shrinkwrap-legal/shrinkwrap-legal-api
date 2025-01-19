package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.python.ShrinkwrapPythonRestService;
import legal.shrinkwrap.api.service.FileHandlingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;

@Configuration
public class CommonServiceConfiguration {

    @Bean
    public HtmlDownloadService htmlDownloadService() {
        return new HtmlDownloadService();
    }

    @Bean
    public FileHandlingService fileHandlingService() { return new FileHandlingService(); }

    @Bean
    public ShrinkwrapPythonRestService pythonRestService() {return new ShrinkwrapPythonRestService(); }
}
