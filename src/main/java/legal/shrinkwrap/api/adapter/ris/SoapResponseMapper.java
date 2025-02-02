package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.*;
import legal.shrinkwrap.api.adapter.ris.dto.RisBvwgMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisDskMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisLvwgMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisVfghMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisVwghMetadaten;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class SoapResponseMapper {

    private static final Logger LOG = LoggerFactory.getLogger(SoapResponseMapper.class);


    public static RisJudikaturResult mapToJudikaturResult(OgdDocumentResults.OgdDocumentReference documentReference) {

        return new RisJudikaturResult(
                mapMetadaten(documentReference.getData().getMetadaten()),
                mapJudikaturMetadaten(documentReference.getData().getMetadaten().getJudikatur()),
                mapUrl(documentReference.getData().getDokumentliste().getContentReference()));

    }

    private static RisMetadaten mapMetadaten(OgdMetadataType ogdMetadataType) {
        return new RisMetadaten(
                ogdMetadataType.getTechnisch().getID(),
                ogdMetadataType.getTechnisch().getApplikation(),
                ogdMetadataType.getTechnisch().getOrgan(),
                formXmlGregorianCalendar(ogdMetadataType.getAllgemein().getVeroeffentlicht().getValue()),
                formXmlGregorianCalendar(ogdMetadataType.getAllgemein().getGeaendert().getValue()),
                ogdMetadataType.getAllgemein().getDokumentUrl()
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

        if(judikaturResponse.getJustiz() != null) judikaturMetadaten.setJustizMetadaten(mapToJustizMetadaten(judikaturResponse.getJustiz()));
        if(judikaturResponse.getBvwg() != null) judikaturMetadaten.setBvwgMetadaten(mapToBvwgMetadaten(judikaturResponse.getBvwg()));
        if(judikaturResponse.getVwgh() != null) judikaturMetadaten.setVwghMetadaten(mapToVwghMetadaten(judikaturResponse.getVwgh()));
        if(judikaturResponse.getVfgh() != null) judikaturMetadaten.setVfghMetadaten(mapToVfghMetadaten(judikaturResponse.getVfgh()));
        if(judikaturResponse.getLvwg() != null) judikaturMetadaten.setLvwgMetadaten(mapToLvwgMetadaten(judikaturResponse.getLvwg()));
        if(judikaturResponse.getDsk() != null) judikaturMetadaten.setDskMetadaten(mapToDskMetadaten(judikaturResponse.getDsk()));

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
        return new RisVfghMetadaten(vfghResponse.getGericht());
    }

    private static RisVwghMetadaten mapToVwghMetadaten(VwghResponse vwghResponse) {
        // TODO map all metadata
        return new RisVwghMetadaten(vwghResponse.getGericht());
    }

    private static RisLvwgMetadaten mapToLvwgMetadaten(LvwgResponse lvwgResponse) {
        // TODO map all metadata
        return new RisLvwgMetadaten(lvwgResponse.getGericht());
    }

    private static RisDskMetadaten mapToDskMetadaten(DskResponse dskResponse) {
        // TODO map all metadata
        return new RisDskMetadaten();
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
