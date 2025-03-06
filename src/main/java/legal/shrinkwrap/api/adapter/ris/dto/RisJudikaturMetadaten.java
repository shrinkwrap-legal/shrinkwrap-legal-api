package legal.shrinkwrap.api.adapter.ris.dto;

import at.gv.bka.ris.v26.soap.ws.client.JudikaturDokumenttyp;
import legal.shrinkwrap.api.adapter.ris.RisJustizMetadaten;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class RisJudikaturMetadaten extends AbstractRisMetadaten {

    private List<String> geschaeftszahl;
    private LocalDate entscheidungsdatum;
    private String ecli;
    private JudikaturDokumenttyp dokumenttyp;
    private String schlagworte;
    private List<String> normen;

    private RisJustizMetadaten justizMetadaten;
    private RisBvwgMetadaten bvwgMetadaten;
    private RisVfghMetadaten vfghMetadaten;
    private RisVwghMetadaten vwghMetadaten;
    private RisLvwgMetadaten lvwgMetadaten;
    private RisDskMetadaten dskMetadaten;
    private RisGbkMetadaten gbkMetadaten;


    public RisJudikaturMetadaten(List<String> geschaeftszahl, JudikaturDokumenttyp dokumenttyp, LocalDate entscheidungsdatum, String ecli,
                                 String schlagworte, List<String> normen) {
        this.geschaeftszahl = geschaeftszahl;
        this.dokumenttyp = dokumenttyp;
        this.entscheidungsdatum = entscheidungsdatum;
        this.ecli = ecli;
        this.schlagworte = schlagworte;
        this.normen = normen;
    }

}
