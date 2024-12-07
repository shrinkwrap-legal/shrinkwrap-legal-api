package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import legal.shrinkwrap.api.adapter.exception.AdapterRequestException;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class RisSoapAdapterImpl implements RisSoapAdapter {
    private final static Logger LOG = LoggerFactory.getLogger(RisSoapAdapterImpl.class);
    private final String STATUS_OK = "ok";

    private final OgdRisServiceSoap risSoap;
    private final JAXBContext soapContext;
    private final ObjectFactory objectFactory = new ObjectFactory();

    public RisSoapAdapterImpl() {
        try {
            soapContext = JAXBContext.newInstance("at.gv.bka.ris.v26.soap.ws.client");
        } catch (JAXBException e) {
            throw new RuntimeException("RIS Soap service could not initialized",e);
        }
        OgdRisService service = new OgdRisService();
        this.risSoap = service.getOgdRisServiceSoap();
    }

    @Override
    public String getVersion() {
        return risSoap.version();
    }

    public RisSearchResult search() {
        /*
        BundesrechtSearchRequest bundesrechtSearchRequest = objectFactory.createBundesrechtSearchRequest();
        risSearch.setBundesrecht(bundesrechtSearchRequest);
        FulltextSearchExpression expression = objectFactory.createFulltextSearchExpression();
        expression.setValue("asvg");
        bundesrechtSearchRequest.setSuchworte(expression);
        bundesrechtSearchRequest.setBrKons(objectFactory.createBrKonsSearchRequest());
        bundesrechtSearchRequest.setDokumenteProSeite(PageSize.ONE_HUNDRED);
        bundesrechtSearchRequest.setSeitennummer(1);
         */
        return new RisSearchResult();
    }

    public RisSearchResult findCaseLawDocuments(RisSearchParameterCaseLaw searchParameter) {
        OGDRisRequest risRequest = SoapRequestMapper.createRisSearch(objectFactory);
        JudikaturSearchRequest judikaturSearchRequest = objectFactory.createJudikaturSearchRequest();
        risRequest.getSuche().setJudikatur(judikaturSearchRequest);

        if(searchParameter.ecli() != null) {
            FulltextSearchExpression searchExpression = objectFactory.createFulltextSearchExpression();
            searchExpression.setValue(searchParameter.ecli());
            judikaturSearchRequest.setSuchworte(searchExpression);
        }

        JudikaturTypSucheinschraenkung judikaturTyp = objectFactory.createJudikaturTypSucheinschraenkung();
        judikaturTyp.setSucheInRechtssaetzen(searchParameter.judikaturTyp().inRechtssaetzen());
        judikaturTyp.setSucheInEntscheidungstexten(searchParameter.judikaturTyp().inEntscheidungstexten());
        judikaturSearchRequest.setDokumenttyp(judikaturTyp);

        switch (searchParameter.court()) {
            case Justiz -> {
                JustizSearchRequest justizSearchRequest = objectFactory.createJustizSearchRequest();
                judikaturSearchRequest.setJustiz(justizSearchRequest);
            }
            case VfGH -> {
                VfghSearchRequest vfghSearchRequest = objectFactory.createVfghSearchRequest();
                judikaturSearchRequest.setVfgh(vfghSearchRequest);
            }
            case VwGH -> {
                VwghSearchRequest vwghSearchRequest = objectFactory.createVwghSearchRequest();
                judikaturSearchRequest.setVwgh(vwghSearchRequest);
            }
            case LVwG -> {
                LvwgSearchRequest lvwgSearchRequest = objectFactory.createLvwgSearchRequest();
                judikaturSearchRequest.setLvwg(lvwgSearchRequest);
            }
            case BVwG -> {
                BvwgSearchRequest bvwgSearchRequest = objectFactory.createBvwgSearchRequest();
                judikaturSearchRequest.setBvwg(bvwgSearchRequest);
            }
            case DSB -> {
                DskSearchRequest dskSearchRequest = objectFactory.createDskSearchRequest();
                judikaturSearchRequest.setDsk(dskSearchRequest);
            }
            case null, default -> {
                throw new AdapterRequestException("Unknown court type "+searchParameter.court());
            }
        }

        judikaturSearchRequest.setDokumenteProSeite(PageSize.TWENTY);
        judikaturSearchRequest.setSeitennummer(1);

        SearchDocumentsResponse.SearchDocumentsResult searchDocumentsResult = searchPagination(risRequest);

        List<RisJudikaturResult> judikaturResults =  searchDocumentsResult.getOgdDocumentResults().getOgdDocumentReference().stream().map(SoapResponseMapper::mapToJudikaturResult).toList();


        return new RisSearchResult(judikaturResults);
    }





    private SearchDocumentsResponse.SearchDocumentsResult searchPagination(OGDRisRequest risRequest) {
        SearchDocumentsResponse.SearchDocumentsResult searchDocumentsResult = risSoap.searchDocuments(risRequest);
        if(searchDocumentsResult.getError() != null) {
            LOG.error("RIS adapter search request error: {}",searchDocumentsResult.getError());
            throw new AdapterRequestException(searchDocumentsResult.getError().getMessage());
        }
        return searchDocumentsResult;
        // searchDocumentsResult.getOgdDocumentResults().getHits().get
    }


}
