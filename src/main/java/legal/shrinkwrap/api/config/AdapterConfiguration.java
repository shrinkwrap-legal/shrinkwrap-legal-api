package legal.shrinkwrap.api.config;


import legal.shrinkwrap.api.adapter.ris.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.RisAdapterImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdapterConfiguration {


    @Bean
    public RisAdapter risAdapter() {
        return new RisAdapterImpl();
    }
}
