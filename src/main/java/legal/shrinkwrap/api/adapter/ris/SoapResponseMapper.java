package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.*;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisMetadaten;
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
        // if(judikaturResponse.getBvwg() != null)
        // if(judikaturResponse.getVwgh() != null) {}
        // if(judikaturResponse.getVfgh() != null) {}

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
