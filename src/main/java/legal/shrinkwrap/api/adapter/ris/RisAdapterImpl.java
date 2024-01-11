package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.dto.OgdQueryParam;
import legal.shrinkwrap.api.adapter.ris.dto.OgdSearchResponse;
import legal.shrinkwrap.api.adapter.ris.dto.OgdSearchResult;
import legal.shrinkwrap.api.adapter.ris.dto.OgdVersionResponse;
import legal.shrinkwrap.api.adapter.ris.dto.enums.OgdApplikationEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class RisAdapterImpl implements RisAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(RisAdapterImpl.class);
    private static final String RIS_BASE_URL = "https://data.bka.gv.at";

    private static final String RIS_API = "/ris/api/v2.6";

    private static final String RIS_VERSION_INFO = "/version";
    private static final String RIS_APP_JUDIKATUR = "/Judikatur?Applikation={Applikation}&Rechtssatznummer={Rechtssatznummer}";

    RestClient restClient = RestClient.create(RIS_BASE_URL + RIS_API);


    @Override
    public String getVersion() {

        OgdVersionResponse response = restClient.get()
                .uri(RIS_VERSION_INFO)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OgdVersionResponse.class);

        return response.ogdSearchResult().version();

    }


    public OgdSearchResult getJustiz(OgdApplikationEnum app, String rechtssatznummer) {

        OgdSearchResponse response = restClient.get()
                .uri(RIS_APP_JUDIKATUR, Map.of(
                        OgdQueryParam.APPLIKATION, app.getTechnisch(),
                        OgdQueryParam.RECHTSSATZNUMMER, rechtssatznummer)
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OgdSearchResponse.class);

        return response.ogdSearchResult();

    }


}
