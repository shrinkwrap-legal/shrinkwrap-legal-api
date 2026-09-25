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

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import legal.shrinkwrap.api.adapter.exception.AdapterRequestException;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RisSoapAdapterImpl implements RisSoapAdapter {
    private final static Logger LOG = LoggerFactory.getLogger(RisSoapAdapterImpl.class);
    private final Long MAX_SIZE = 1000000L;
    private static final ZoneId RIS_ZONE = ZoneId.of("Europe/Vienna");
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
                if (r.getJudikaturMetadaten() == null || r.getJudikaturMetadaten().getDokumenttyp() == null) {
                    if (searchParameter.court() == RisCourt.GBK) {
                        return true;
                    }
                    return false;
                }
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





    @Override
    public List<RisNormResult> findNormDocuments(RisSearchParameterNorm searchParameter) {
        OGDRisRequest risRequest = SoapRequestMapper.createRisSearch(objectFactory);

        NormabschnittSucheinschraenkung abschnitt = null;
        if (searchParameter.abschnittTyp() != null) {
            abschnitt = objectFactory.createNormabschnittSucheinschraenkung();
            abschnitt.setTyp(searchParameter.abschnittTyp());
            abschnitt.setVon(searchParameter.abschnittVon());
            abschnitt.setBis(searchParameter.abschnittBis());
        }

        Fassungsangabe fassung = null;
        if (searchParameter.fassungVom() != null) {
            StichtagFassungsangabe stichtag = objectFactory.createStichtagFassungsangabe();
            stichtag.setFassungVom(toXmlDate(searchParameter.fassungVom()));
            fassung = stichtag;
        }

        ExactMatchSearchExpression gesetzesnummer = null;
        if (searchParameter.gesetzesnummer() != null) {
            gesetzesnummer = objectFactory.createExactMatchSearchExpression();
            gesetzesnummer.setValue(searchParameter.gesetzesnummer());
        }

        FulltextSearchExpression titel = null;
        if (searchParameter.titel() != null) {
            titel = objectFactory.createFulltextSearchExpression();
            titel.setValue(searchParameter.titel());
        }

        if (searchParameter.isLandesrecht()) {
            LrKonsSearchRequest lrKons = objectFactory.createLrKonsSearchRequest();
            lrKons.setAbschnitt(abschnitt);
            lrKons.setFassung(fassung);
            lrKons.setGesetzesnummer(gesetzesnummer);
            lrKons.setBundesland(bundeslandRestriction(searchParameter.bundesland()));

            LandesrechtSearchRequest landesrecht = objectFactory.createLandesrechtSearchRequest();
            landesrecht.setLrKons(lrKons);
            landesrecht.setTitel(titel);
            risRequest.getSuche().setLandesrecht(landesrecht);
        } else {
            BrKonsSearchRequest brKons = objectFactory.createBrKonsSearchRequest();
            brKons.setAbschnitt(abschnitt);
            brKons.setFassung(fassung);
            brKons.setGesetzesnummer(gesetzesnummer);

            BundesrechtSearchRequest bundesrecht = objectFactory.createBundesrechtSearchRequest();
            bundesrecht.setBrKons(brKons);
            bundesrecht.setTitel(titel);
            risRequest.getSuche().setBundesrecht(bundesrecht);
        }

        //the sort order is never set on purpose: the RIS sortable columns order "§ 10" before
        //"§ 2", only the default order of the result list is usable
        return searchPagination(risRequest).stream().map(SoapResponseMapper::mapToNormResult).toList();
    }

    @Override
    public List<RisNormResult> findChangedNormDocuments(boolean landesrecht, int changedInLastXDays) {
        OGDRisRequest risRequest = objectFactory.createOGDRisRequest();

        OGDHistoryType history = objectFactory.createOGDHistoryType();
        history.setAnwendung(landesrecht
                ? HistoryRequestApplicationType.LANDESNORMEN
                : HistoryRequestApplicationType.BUNDESNORMEN);
        //a withdrawn document has to reach the import too, it is soft deleted rather than dropped
        history.setIncludeDeletedDocuments(true);
        history.setAenderungenVon(objectFactory.createOGDHistoryTypeAenderungenVon(
                toXmlDate(LocalDate.now(RIS_ZONE).minusDays(changedInLastXDays))));
        history.setAenderungenBis(objectFactory.createOGDHistoryTypeAenderungenBis(
                toXmlDate(LocalDate.now(RIS_ZONE).plusDays(1))));

        //a change request carries no search, the two are mutually exclusive
        risRequest.setAenderungen(history);

        return searchPagination(risRequest).stream()
                //the feed reports withdrawn documents as an entry without any payload at all -
                //no id, no metadata, nothing to import. Measured: 11 of 685 over five days
                .filter(d -> d.getData() != null && d.getData().getMetadaten() != null)
                .map(SoapResponseMapper::mapToNormResult)
                .toList();
    }

    /** The request models the state as nine booleans rather than as the Bundesland enum. */
    private BundeslandSucheinschraenkung bundeslandRestriction(Bundesland bundesland) {
        BundeslandSucheinschraenkung restriction = objectFactory.createBundeslandSucheinschraenkung();
        switch (bundesland) {
            case BURGENLAND -> restriction.setSucheInBurgenland(true);
            case KAERNTEN -> restriction.setSucheInKaernten(true);
            case NIEDEROESTERREICH -> restriction.setSucheInNiederoesterreich(true);
            case OBEROESTERREICH -> restriction.setSucheInOberoesterreich(true);
            case SALZBURG -> restriction.setSucheInSalzburg(true);
            case STEIERMARK -> restriction.setSucheInSteiermark(true);
            case TIROL -> restriction.setSucheInTirol(true);
            case VORARLBERG -> restriction.setSucheInVorarlberg(true);
            case WIEN -> restriction.setSucheInWien(true);
            case null, default -> throw new AdapterRequestException(
                    "A concrete Bundesland is required for a Landesrecht search");
        }
        return restriction;
    }

    private static XMLGregorianCalendar toXmlDate(LocalDate date) {
        try {
            XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault())));
            calendar.setHour(DatatypeConstants.FIELD_UNDEFINED);
            calendar.setMinute(DatatypeConstants.FIELD_UNDEFINED);
            calendar.setSecond(DatatypeConstants.FIELD_UNDEFINED);
            calendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            calendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
            return calendar;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
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
                risRequest.getSuche().getBundesrecht().setDokumenteProSeite(PageSize.ONE_HUNDRED);
                risRequest.getSuche().getBundesrecht().setSeitennummer(seitennummer);
            }
            if(risRequest.getSuche().getLandesrecht() != null) {
                risRequest.getSuche().getLandesrecht().setDokumenteProSeite(PageSize.ONE_HUNDRED);
                risRequest.getSuche().getLandesrecht().setSeitennummer(seitennummer);
            }
        } else if (risRequest != null && risRequest.getAenderungen() != null) {
            risRequest.getAenderungen().setDokumenteProSeite(PageSize.ONE_HUNDRED);
            risRequest.getAenderungen().setSeitennummer(seitennummer);
        }

    }




}
