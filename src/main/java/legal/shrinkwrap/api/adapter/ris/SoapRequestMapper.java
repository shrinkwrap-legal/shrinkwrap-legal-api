package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.OGDRisRequest;
import at.gv.bka.ris.v26.soap.ws.client.OGDSearch;
import at.gv.bka.ris.v26.soap.ws.client.ObjectFactory;

public class SoapRequestMapper {


    public static OGDRisRequest createRisSearch(ObjectFactory objectFactory) {
        OGDRisRequest risRequest = objectFactory.createOGDRisRequest();
        OGDSearch risSearch = objectFactory.createOGDSearch();
        risRequest.setSuche(risSearch);
        return risRequest;
    }
}
