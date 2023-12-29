package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.dto.OgdVersionResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class RisAdapterImpl implements RisAdapter {

    private static final String RIS_BASE_URL = "https://data.bka.gv.at";

    private static final String RIS_API = "/ris/api/v2.6";

    private static final String RIS_APP_JUDIKATUR = "/Judikatur";

    RestClient restClient = RestClient.create(RIS_BASE_URL + RIS_API);


    @Override
    public String getVersion() {

        OgdVersionResponse response = restClient.get()
                .uri("/version")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OgdVersionResponse.class);

        return response.ogdSearchResult().version();

    }


}
