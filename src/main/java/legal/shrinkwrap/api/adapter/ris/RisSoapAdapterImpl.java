package legal.shrinkwrap.api.adapter.ris;

import java.time.*;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import at.gv.bka.ris.v26.soap.ws.client.*;
import com.github.javaparser.utils.Log;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import legal.shrinkwrap.api.adapter.exception.AdapterRequestException;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RisSoapAdapterImpl implements RisSoapAdapter {
    private final static Logger LOG = LoggerFactory.getLogger(RisSoapAdapterImpl.class);
    private final Long MAX_SIZE = 1000000L;
    private final String STATUS_OK = "ok";

    private final OgdRisServiceSoap risSoap;
    private final JAXBContext soapContext;
    private final ObjectFactory objectFactory = new ObjectFactory();

    public RisSoapAdapterImpl() {
        /*
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "999999");

         */
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
        if (searchParameter.docNumber() != null) {
            FulltextSearchExpression searchExpression = objectFactory.createFulltextSearchExpression();
            searchExpression.setValue(searchParameter.docNumber());
            judikaturSearchRequest.setSuchworte(searchExpression);
        }

        if(searchParameter.year() != null) {
            try {
                ZonedDateTime datetimeStart = ZonedDateTime.of(LocalDate.of(searchParameter.year().getValue(), 1,1), LocalTime.MIN, ZoneId.ofOffset("", ZoneOffset.ofHours(1)));
                XMLGregorianCalendar decisionDateStart = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(datetimeStart));
                decisionDateStart.setHour(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateStart.setMinute(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateStart.setSecond(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateStart.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
                JAXBElement<XMLGregorianCalendar> xmlStart = objectFactory.createJudikaturSearchRequestEntscheidungsdatumVon(decisionDateStart);
                judikaturSearchRequest.setEntscheidungsdatumVon(xmlStart);

                ZonedDateTime datetimeEnd = ZonedDateTime.of(LocalDate.of(searchParameter.year().getValue(), 12,31), LocalTime.MAX, ZoneId.ofOffset("", ZoneOffset.ofHours(1)));
                XMLGregorianCalendar decisionDateEnd = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(datetimeEnd));
                decisionDateEnd.setHour(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateEnd.setMinute(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateEnd.setSecond(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateEnd.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
                JAXBElement<XMLGregorianCalendar> xmlEnd = objectFactory.createJudikaturSearchRequestEntscheidungsdatumBis(decisionDateEnd);
                judikaturSearchRequest.setEntscheidungsdatumBis(xmlEnd);
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        }

        //Search by change date
        if (searchParameter.changedInLastXDays() != null) {
            risRequest.setSuche(null); //unsupported in change request

            try {
                OGDHistoryType ogdHistoryType = objectFactory.createOGDHistoryType();
                ogdHistoryType.setIncludeDeletedDocuments(false);
                switch (searchParameter.court()) {
                    case Justiz -> {
                        ogdHistoryType.setAnwendung(HistoryRequestApplicationType.JUSTIZ);
                    }
                    case VwGH -> {
                        ogdHistoryType.setAnwendung(HistoryRequestApplicationType.VWGH);
                    }
                    case VfGH -> {
                        ogdHistoryType.setAnwendung(HistoryRequestApplicationType.VFGH);
                    }
                    case BVwG -> {
                        ogdHistoryType.setAnwendung(HistoryRequestApplicationType.BVWG);
                    }
                    case LVwG -> {
                        ogdHistoryType.setAnwendung(HistoryRequestApplicationType.LVWG);
                    }
                    case DSB -> {
                        ogdHistoryType.setAnwendung(HistoryRequestApplicationType.DSK);
                    }
                    case GBK -> {
                        ogdHistoryType.setAnwendung(HistoryRequestApplicationType.GBK);
                    }
                }

                ZonedDateTime datetimeStart = ZonedDateTime.now(ZoneId.of("Europe/Vienna")).minusDays(searchParameter.changedInLastXDays());
                XMLGregorianCalendar decisionDateStart = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(datetimeStart));
                decisionDateStart.setHour(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateStart.setMinute(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateStart.setSecond(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateStart.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
                JAXBElement<XMLGregorianCalendar> xmlStart = objectFactory.createOGDHistoryTypeAenderungenVon(decisionDateStart);
                ogdHistoryType.setAenderungenVon(xmlStart);

                ZonedDateTime datetimeEnd = ZonedDateTime.now(ZoneId.of("Europe/Vienna")).plusDays(1);
                XMLGregorianCalendar decisionDateEnd = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(datetimeEnd));
                decisionDateEnd.setHour(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateEnd.setMinute(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateEnd.setSecond(DatatypeConstants.FIELD_UNDEFINED);
                decisionDateEnd.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
                JAXBElement<XMLGregorianCalendar> xmlEnd = objectFactory.createOGDHistoryTypeAenderungenBis(decisionDateEnd);
                ogdHistoryType.setAenderungenBis(xmlEnd);
                risRequest.setAenderungen(ogdHistoryType);
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        }


        if (searchParameter.changedInLastXDays() == null) {

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
                case GBK -> {
                    GbkSearchRequest gbkSearchRequest = objectFactory.createGbkSearchRequest();
                    judikaturSearchRequest.setGbk(gbkSearchRequest);
                }
                case null, default -> {
                    throw new AdapterRequestException("Unknown court type " + searchParameter.court());
                }
            }
        }

        List<OgdDocumentResults.OgdDocumentReference> documentResults = searchPagination(risRequest);

        List<RisJudikaturResult> judikaturResults =  documentResults.stream().map(SoapResponseMapper::mapToJudikaturResult).toList();

        //for changedSince, we're not able to filter by rechtssatz/entscheidung in request, so we have to
        //do afterward
        if (searchParameter.changedInLastXDays() != null && searchParameter.judikaturTyp() != null) {
            judikaturResults = judikaturResults.stream().filter(r -> {
                switch (r.getJudikaturMetadaten().getDokumenttyp()) {
                    case TEXT -> {
                        return searchParameter.judikaturTyp().inEntscheidungstexten();
                    }
                    case RECHTSSATZ -> {
                        return searchParameter.judikaturTyp().inRechtssaetzen();
                    }
                }
                return false;
            }).collect(Collectors.toList());
            LOG.info("filtered to " + judikaturResults.size());
        }

        return new RisSearchResult(judikaturResults);
    }





    private List<OgdDocumentResults.OgdDocumentReference> searchPagination(OGDRisRequest risRequest) {
        List<OgdDocumentResults.OgdDocumentReference> documents = new ArrayList<>();

        Integer pageCount = 1;
        Long resultSize = 0L;

        do {
            incrementSeitennummer(risRequest, pageCount);
            SearchDocumentsResponse.SearchDocumentsResult searchDocumentsResult = search(risRequest);
            documents.addAll(searchDocumentsResult.getOgdDocumentResults().getOgdDocumentReference());
            resultSize = searchDocumentsResult.getOgdDocumentResults().getHits().getValue().longValue();
            pageCount++;
            LOG.info(documents.size() + " documents found in page " + pageCount + " of " + resultSize + " - " + (documents.size()*100 / (Math.max(resultSize,1))) + " %");
        } while (documents.size() < resultSize && documents.size() < MAX_SIZE);

        return documents;

    }

    private SearchDocumentsResponse.SearchDocumentsResult search(OGDRisRequest risRequest) {
        SearchDocumentsResponse.SearchDocumentsResult searchDocumentsResult = risSoap.searchDocuments(risRequest);
        if(searchDocumentsResult.getError() != null) {
            LOG.error("RIS adapter search request error: {}",searchDocumentsResult.getError());
            throw new AdapterRequestException(searchDocumentsResult.getError().getMessage());
        }

        return searchDocumentsResult;

    }

    private void incrementSeitennummer(OGDRisRequest risRequest, Integer seitennummer) {
        if(risRequest != null && risRequest.getSuche() != null) {
            if(risRequest.getSuche().getJudikatur() != null) {
                risRequest.getSuche().getJudikatur().setDokumenteProSeite(PageSize.ONE_HUNDRED);
                risRequest.getSuche().getJudikatur().setSeitennummer(seitennummer);
            }
            if(risRequest.getSuche().getBundesrecht() != null) {
                risRequest.getSuche().getBundesrecht().setSeitennummer(seitennummer);
            }
        } else if (risRequest != null && risRequest.getAenderungen() != null) {
            risRequest.getAenderungen().setDokumenteProSeite(PageSize.ONE_HUNDRED);
            risRequest.getAenderungen().setSeitennummer(seitennummer);
        }

    }




}
