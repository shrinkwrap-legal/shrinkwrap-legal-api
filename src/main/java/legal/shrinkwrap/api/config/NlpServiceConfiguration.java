package legal.shrinkwrap.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import legal.shrinkwrap.api.service.NLPService;
import legal.shrinkwrap.api.service.OpenNlpServiceImpl;

// @Configuration
public class NlpServiceConfiguration {

    @Bean("coreNlpService")
    public NLPService coreNlpService() {
        return null;// new CoreNlpServiceImpl();
    }

    @Bean("openNlpService")
    public NLPService openNLPService() {
        return new OpenNlpServiceImpl();
    }
}
