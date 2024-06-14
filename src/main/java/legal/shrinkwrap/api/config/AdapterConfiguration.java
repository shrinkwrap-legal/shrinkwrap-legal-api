package legal.shrinkwrap.api.config;


import legal.shrinkwrap.api.adapter.ris.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.RisAdapterImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Open API description
 * <a href="https://data.bka.gv.at/ris/api/v2.6/Content/OGD-RIS_API.yml">OGD RIS API</a>
 */
@Configuration
public class AdapterConfiguration {


    @Bean
    public RisAdapter risAdapter() {
        return new RisAdapterImpl();
    }
}
