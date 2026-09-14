package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import legal.shrinkwrap.api.adapter.ris.dto.*;
import legal.shrinkwrap.api.utils.ObjectMapperWithXmlGregorianCalenderSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBElement;
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
        return new RisDskMetadaten(dskResponse.getEntscheidendeBehoerde(),
                dskResponse.getEntscheidungsart().value());
    }

    private static RisGbkMetadaten mapToGbkMetadaten(GbkResponse gbkResponse) {
        // TODO map all metadata
        return new RisGbkMetadaten(gbkResponse.getKommission(), gbkResponse.getEntscheidungsart().value());
    }

    public static RisNormResult mapToNormResult(OgdDocumentResults.OgdDocumentReference documentReference) {
        OgdMetadataType metadaten = documentReference.getData().getMetadaten();
        //the change feed delivers documents without a document list
        List<WebDocumentContentReference> contentReferences =
                documentReference.getData().getDokumentliste() == null
                        ? List.of()
                        : documentReference.getData().getDokumentliste().getContentReference();

        return new RisNormResult(
                mapMetadaten(metadaten),
                mapNormMetadaten(metadaten),
                mapUrl(contentReferences),
                mapAttachments(contentReferences));
    }

    /**
     * BrKons and LrKons carry the same fields but share no common type, so both are read out
     * separately. Everything not mapped here still reaches the caller through
     * {@link RisMetadaten#getFullResponseAsJson()}.
     */
    private static RisNormMetadaten mapNormMetadaten(OgdMetadataType metadaten) {
        RisNormMetadaten norm = new RisNormMetadaten();

        if (metadaten.getBundesrecht() != null && metadaten.getBundesrecht().getBrKons() != null) {
            BundesrechtResponse bundesrecht = metadaten.getBundesrecht();
            BrKonsResponse brKons = bundesrecht.getBrKons();

            norm.setKurztitel(bundesrecht.getKurztitel());
            norm.setTitel(bundesrecht.getTitel());
            norm.setEli(bundesrecht.getEli());

            norm.setGesetzesnummer(brKons.getGesetzesnummer());
            norm.setAbkuerzung(brKons.getAbkuerzung());
            norm.setDokumenttyp(brKons.getDokumenttyp());
            norm.setArtikelParagraphAnlage(brKons.getArtikelParagraphAnlage());
            norm.setArtikelnummer(unwrap(brKons.getArtikelnummer()));
            norm.setArtikelbuchstabe(brKons.getArtikelbuchstabe());
            norm.setParagraphnummer(unwrap(brKons.getParagraphnummer()));
            norm.setParagraphbuchstabe(brKons.getParagraphbuchstabe());
            norm.setAnlagennummer(brKons.getAnlagennummer());
            norm.setAnlagenbuchstabe(brKons.getAnlagenbuchstabe());
            norm.setAnlagenteil(brKons.getAnlagenteil());
            norm.setKundmachungsorgan(brKons.getKundmachungsorgan());
            norm.setTyp(brKons.getTyp());
            norm.setStammnormPublikationsorgan(brKons.getStammnormPublikationsorgan());
            norm.setStammnormBgblnummer(brKons.getStammnormBgblnummer());
            norm.setNovellenPublikationsorgan(brKons.getNovellenPublikationsorgan());
            norm.setNovellenBgblnummer(brKons.getNovellenBgblnummer());
            norm.setNovellenBeziehung(brKons.getNovellenBeziehung());
            norm.setUebergangsrecht(brKons.getUebergangsrecht());
            norm.setInkrafttreten(formXmlGregorianCalendar(unwrap(brKons.getInkrafttretensdatum())));
            norm.setAusserkrafttreten(formXmlGregorianCalendar(unwrap(brKons.getAusserkrafttretensdatum())));
            norm.setBeachte(brKons.getBeachte());
            norm.setAnmerkung(brKons.getAnmerkung());
            norm.setAenderung(brKons.getAenderung());
            norm.setSchlagworte(brKons.getSchlagworte());
            norm.setIndizes(fromArrayOfStrings(brKons.getIndizes()));
            norm.setGesamteRechtsvorschriftUrl(brKons.getGesamteRechtsvorschriftUrl());

        } else if (metadaten.getLandesrecht() != null && metadaten.getLandesrecht().getLrKons() != null) {
            LandesrechtResponse landesrecht = metadaten.getLandesrecht();
            LrKonsResponse lrKons = landesrecht.getLrKons();

            norm.setKurztitel(landesrecht.getKurztitel());
            norm.setTitel(landesrecht.getTitel());
            //unlike BrKons, LrKons carries the eli on the application level
            norm.setEli(lrKons.getEli());
            norm.setBundesland(landesrecht.getBundesland());

            norm.setGesetzesnummer(lrKons.getGesetzesnummer());
            norm.setAbkuerzung(lrKons.getAbkuerzung());
            norm.setDokumenttyp(lrKons.getDokumenttyp());
            norm.setArtikelParagraphAnlage(lrKons.getArtikelParagraphAnlage());
            norm.setArtikelnummer(unwrap(lrKons.getArtikelnummer()));
            norm.setArtikelbuchstabe(lrKons.getArtikelbuchstabe());
            norm.setParagraphnummer(unwrap(lrKons.getParagraphnummer()));
            norm.setParagraphbuchstabe(lrKons.getParagraphbuchstabe());
            norm.setAnlagennummer(lrKons.getAnlagennummer());
            norm.setAnlagenbuchstabe(lrKons.getAnlagenbuchstabe());
            norm.setAnlagenteil(lrKons.getAnlagenteil());
            norm.setKundmachungsorgan(lrKons.getKundmachungsorgan());
            norm.setTyp(lrKons.getTyp());
            norm.setStammnormPublikationsorgan(lrKons.getStammnormPublikationsorgan());
            norm.setStammnormBgblnummer(lrKons.getStammnormBgblnummer());
            norm.setNovellenPublikationsorgan(lrKons.getNovellenPublikationsorgan());
            norm.setNovellenBgblnummer(lrKons.getNovellenBgblnummer());
            norm.setNovellenBeziehung(lrKons.getNovellenBeziehung());
            norm.setUebergangsrecht(lrKons.getUebergangsrecht());
            norm.setInkrafttreten(formXmlGregorianCalendar(unwrap(lrKons.getInkrafttretensdatum())));
            norm.setAusserkrafttreten(formXmlGregorianCalendar(unwrap(lrKons.getAusserkrafttretensdatum())));
            norm.setBeachte(lrKons.getBeachte());
            norm.setAnmerkung(lrKons.getAnmerkung());
            norm.setAenderung(lrKons.getAenderung());
            norm.setSchlagworte(lrKons.getSchlagworte());
            norm.setIndizes(fromArrayOfStrings(lrKons.getIndizes()));
            norm.setGesamteRechtsvorschriftUrl(lrKons.getGesamteRechtsvorschriftUrl());
        }

        return norm;
    }

    /**
     * Everything that is not the main document: the images of § 50 StVO and the pdf annexes of
     * laws such as the KEM-V, whose html does not reference them at all.
     */
    private static List<RisNormAttachment> mapAttachments(List<WebDocumentContentReference> referenceList) {
        if (referenceList == null) {
            return List.of();
        }
        return referenceList.stream()
                .filter(r -> !WebDocumentContentType.MAIN_DOCUMENT.equals(r.getContentType()))
                .filter(r -> r.getUrls() != null && r.getUrls().getContentUrl() != null)
                .flatMap(r -> r.getUrls().getContentUrl().stream()
                        .map(url -> new RisNormAttachment(r.getContentType(), r.getName(),
                                url.getDataType(), url.getUrl())))
                .toList();
    }

    private static <T> T unwrap(JAXBElement<T> element) {
        return element == null ? null : element.getValue();
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
