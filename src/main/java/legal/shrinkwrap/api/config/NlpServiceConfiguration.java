package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.service.CoreNlpServiceImpl;
import legal.shrinkwrap.api.service.NLPService;
import legal.shrinkwrap.api.service.OpenNlpServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
