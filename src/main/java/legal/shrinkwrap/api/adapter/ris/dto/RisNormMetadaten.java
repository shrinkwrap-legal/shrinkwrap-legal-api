package legal.shrinkwrap.api.adapter.ris.dto;

import at.gv.bka.ris.v26.soap.ws.client.NormDokumenttyp;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Metadata of a single norm document. BrKons (federal) and LrKons (state) carry the same fields,
 * so both map onto this class; {@link #bundesland} is null for federal law.
 */
@Setter
@Getter
@NoArgsConstructor
public class RisNormMetadaten extends AbstractRisMetadaten {

    /** As delivered by RIS ("Wien", "Tirol"); null for federal law. */
    private String bundesland;
    private String gesetzesnummer;
    private String kurztitel;
    private String titel;
    private String abkuerzung;

    /** ELI of the provision - the stable key across versions, see the norm concept document. */
    private String eli;

    private NormDokumenttyp dokumenttyp;

    /** Full designation as RIS writes it: "§ 50", "Art. 2", "Anl. 2/49". */
    private String artikelParagraphAnlage;

    private Integer artikelnummer;
    private String artikelbuchstabe;
    private Integer paragraphnummer;
    private String paragraphbuchstabe;
    private String anlagennummer;
    private String anlagenbuchstabe;

    /** Carries the distinction on its own where a law numbers annexes as "Anl. 2/01" … "2/49". */
    private String anlagenteil;

    private String kundmachungsorgan;
    private String typ;
    private String stammnormPublikationsorgan;
    private String stammnormBgblnummer;
    private String novellenPublikationsorgan;
    private String novellenBgblnummer;
    private String novellenBeziehung;

    /** A marker rather than a flag: "ÜR" for transitional law, but also "EG/EU", "A", "S". */
    private String uebergangsrecht;

    private LocalDate inkrafttreten;

    /** Frequently absent - an in-force provision has no end date. */
    private LocalDate ausserkrafttreten;

    private String beachte;
    private String anmerkung;
    private String aenderung;
    private String schlagworte;
    private List<String> indizes;
    private String gesamteRechtsvorschriftUrl;
}
