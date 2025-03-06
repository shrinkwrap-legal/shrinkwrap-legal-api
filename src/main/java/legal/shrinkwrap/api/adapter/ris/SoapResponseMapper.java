package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import legal.shrinkwrap.api.adapter.ris.dto.*;
import legal.shrinkwrap.api.utils.ObjectMapperWithXmlGregorianCalenderSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class SoapResponseMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SoapResponseMapper.class);
    private static final ObjectMapper MAPPER = new ObjectMapperWithXmlGregorianCalenderSupport();


    public static RisJudikaturResult mapToJudikaturResult(OgdDocumentResults.OgdDocumentReference documentReference) {

        return new RisJudikaturResult(
                mapMetadaten(documentReference.getData().getMetadaten()),
                mapJudikaturMetadaten(documentReference.getData().getMetadaten().getJudikatur()),
                mapUrl(documentReference.getData().getDokumentliste().getContentReference()));

    }

    private static RisMetadaten mapMetadaten(OgdMetadataType ogdMetadataType) {
        String json = null;
        try {
             json = MAPPER.writeValueAsString(ogdMetadataType);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage());
        }

        LocalDate veroeffentlicht = null;
        if (ogdMetadataType.getAllgemein().getVeroeffentlicht() != null) {
            veroeffentlicht = formXmlGregorianCalendar(ogdMetadataType.getAllgemein().getVeroeffentlicht().getValue());
        }

        LocalDate geaendert = null;
        if (ogdMetadataType.getAllgemein().getGeaendert() != null) {
            geaendert = formXmlGregorianCalendar(ogdMetadataType.getAllgemein().getGeaendert().getValue());
        }

        return new RisMetadaten(
                ogdMetadataType.getTechnisch().getID(),
                ogdMetadataType.getTechnisch().getApplikation(),
                ogdMetadataType.getTechnisch().getOrgan(),
                veroeffentlicht,
                geaendert,
                ogdMetadataType.getAllgemein().getDokumentUrl(),
                json
        );
    }

    private static RisJudikaturMetadaten mapJudikaturMetadaten(JudikaturResponse judikaturResponse) {

        RisJudikaturMetadaten judikaturMetadaten = new RisJudikaturMetadaten(
                judikaturResponse.getGeschaeftszahl().getItem(),
                judikaturResponse.getDokumenttyp(),
                formXmlGregorianCalendar(judikaturResponse.getEntscheidungsdatum().getValue()),
                judikaturResponse.getEuropeanCaseLawIdentifier(),
                judikaturResponse.getSchlagworte(),
                fromArrayOfStrings(judikaturResponse.getNormen())
        );

        if (judikaturResponse.getJustiz() != null)
            judikaturMetadaten.setJustizMetadaten(mapToJustizMetadaten(judikaturResponse.getJustiz()));
        if (judikaturResponse.getBvwg() != null)
            judikaturMetadaten.setBvwgMetadaten(mapToBvwgMetadaten(judikaturResponse.getBvwg()));
        if (judikaturResponse.getVwgh() != null)
            judikaturMetadaten.setVwghMetadaten(mapToVwghMetadaten(judikaturResponse.getVwgh()));
        if (judikaturResponse.getVfgh() != null)
            judikaturMetadaten.setVfghMetadaten(mapToVfghMetadaten(judikaturResponse.getVfgh()));
        if (judikaturResponse.getLvwg() != null)
            judikaturMetadaten.setLvwgMetadaten(mapToLvwgMetadaten(judikaturResponse.getLvwg()));
        if (judikaturResponse.getDsk() != null)
            judikaturMetadaten.setDskMetadaten(mapToDskMetadaten(judikaturResponse.getDsk()));
        if (judikaturResponse.getGbk() != null)
            judikaturMetadaten.setGbkMetadaten(mapToGbkMetadaten(judikaturResponse.getGbk()));
        return judikaturMetadaten;
    }

    private static RisJustizMetadaten mapToJustizMetadaten(JustizResponse justizResponse) {

        // justizResponse.getTextnummern();
        // justizResponse.getEntscheidungstexte().getItem().getFirst().get

        return new RisJustizMetadaten(
                justizResponse.getGericht(),
                justizResponse.getEntscheidungsart(),
                justizResponse.getAnmerkung(),
                justizResponse.getFundstelle(),
                fromArrayOfStrings(justizResponse.getRechtssatznummern()),
                fromArrayOfStrings(justizResponse.getRechtsgebiete())
        );
    }

    private static RisBvwgMetadaten mapToBvwgMetadaten(BvwgResponse bvwgResponse) {
        // TODO map all metadata
        return new RisBvwgMetadaten(bvwgResponse.getGericht());
    }

    private static RisVfghMetadaten mapToVfghMetadaten(VfghResponse vfghResponse) {
        // TODO map all metadata
        return new RisVfghMetadaten(vfghResponse.getGericht(),
                vfghResponse.getEntscheidungsart().value());
    }

    private static RisVwghMetadaten mapToVwghMetadaten(VwghResponse vwghResponse) {
        // TODO map all metadata
        return new RisVwghMetadaten(vwghResponse.getGericht(),
                vwghResponse.getEntscheidungsart().value());
    }

    private static RisLvwgMetadaten mapToLvwgMetadaten(LvwgResponse lvwgResponse) {
        // TODO map all metadata
        return new RisLvwgMetadaten(lvwgResponse.getGericht(),
                lvwgResponse.getEntscheidungsart().value());
    }

    private static RisDskMetadaten mapToDskMetadaten(DskResponse dskResponse) {
        // TODO map all metadata
        return new RisDskMetadaten(dskResponse.getEntscheidungsart().value());
    }

    private static RisGbkMetadaten mapToGbkMetadaten(GbkResponse gbkResponse) {
        // TODO map all metadata
        return new RisGbkMetadaten(gbkResponse.getEntscheidungsart().value());
    }

    private static String mapUrl(List<WebDocumentContentReference> referenceList) {

        WebDocumentContentReference reference = referenceList.stream().filter(r -> r.getContentType().equals(WebDocumentContentType.MAIN_DOCUMENT)).findFirst().orElse(null);
        if(reference != null) {
            var url = reference.getUrls().getContentUrl().stream().filter(r -> r.getDataType().equals(WebDocumentDataType.HTML)).findFirst().orElse(null);
            if(url != null) {
                return url.getUrl();
            }
        }
        return null;
    }

    private static LocalDate formXmlGregorianCalendar(XMLGregorianCalendar xmlGregorianCalendar) {
        if(xmlGregorianCalendar == null) {
            return null;
        }
        return LocalDate.ofInstant(xmlGregorianCalendar.toGregorianCalendar().getTime().toInstant(), ZoneId.systemDefault());
    }

    public static List<String> fromArrayOfStrings(ArrayOfString arrayOfString) {
        if(arrayOfString == null) {
            return List.of();
        }
        return arrayOfString.getItem();

    }

}
