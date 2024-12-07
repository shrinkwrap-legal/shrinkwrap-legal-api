package legal.shrinkwrap.api.config;


import legal.shrinkwrap.api.adapter.ris.RisSoapAdapterImpl;
import legal.shrinkwrap.api.adapter.ris.rest.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.rest.RisAdapterImpl;
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

    @Bean
    public RisSoapAdapterImpl risSoapAdapter() {
        return new RisSoapAdapterImpl();
    }

    /*
    @Bean
    public ApiClient apiClient() {
        ApiClient client = new ApiClient();
        client.setDebugging(true);
        return client;
    }

    @Bean
    public JudikaturRisApi judikaturRisApi() {
        return new JudikaturRisApi(apiClient());
    }

     */

}
