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
    private static final String RIS_URL = "https://www.ris.bka.gv.at";

    private static final String RIS_API = "/ris/api/v2.6";

    private static final String RIS_VERSION_INFO = "/version";
    private static final String RIS_APP_JUDIKATUR = "/Judikatur?Applikation={Applikation}&Rechtssatznummer={Rechtssatznummer}";

    private static final String RIS_JUDIKATOR_HTML = "/Dokumente/{Applikation}/{Dokumentennummer}/{Dokumentennummer}.html";
    private static final String RIS_APP_JUDIKATUR_DOCNUMBER = "/Judikatur?Applikation={Applikation}&Suchworte={Suchworte}&SucheNachRechtssatz=True&SucheNachText=True";


    RestClient restClient = RestClient.create(RIS_BASE_URL + RIS_API);

    RestClient restClientDocs = RestClient.create(RIS_URL);


    @Override
    public String getVersion() {

        OgdVersionResponse response = restClient.get()
                .uri(RIS_VERSION_INFO)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OgdVersionResponse.class);

        return response.ogdSearchResult().version();

    }



    @Override
    public String getCaselawByDocNumberAsHtml(OgdApplikationEnum app, String docNumber) {

        String response = restClientDocs.get()
                .uri(RIS_JUDIKATOR_HTML, Map.of(
                        OgdQueryParam.APPLIKATION, app.getTechnisch(),
                        OgdQueryParam.DOCNUMBER, docNumber)
                )
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .body(String.class);

        return response;
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
