package legal.shrinkwrap.api.adapter.ris.dto;

import at.gv.bka.ris.v26.soap.ws.client.JudikaturDokumenttyp;
import legal.shrinkwrap.api.adapter.ris.RisJustizMetadaten;

import java.time.LocalDate;
import java.util.List;

public class RisJudikaturMetadaten extends AbstractRisMetadaten {

    private List<String> geschaeftszahl;
    private LocalDate entscheidungsdatum;
    private String ecli;
    private JudikaturDokumenttyp dokumenttyp;
    private String schlagworte;
    private List<String> normen;

    private RisJustizMetadaten justizMetadaten;

    public RisJudikaturMetadaten(List<String> geschaeftszahl, JudikaturDokumenttyp dokumenttyp, LocalDate entscheidungsdatum, String ecli,
                                 String schlagworte, List<String> normen) {
        this.geschaeftszahl = geschaeftszahl;
        this.dokumenttyp = dokumenttyp;
        this.entscheidungsdatum = entscheidungsdatum;
        this.ecli = ecli;
        this.schlagworte = schlagworte;
        this.normen = normen;
    }

    public List<String> getGeschaeftszahl() {
        return geschaeftszahl;
    }

    public JudikaturDokumenttyp getDokumenttyp() {
        return dokumenttyp;
    }

    public LocalDate getEntscheidungsdatum() {
        return entscheidungsdatum;
    }

    public String getEcli() {
        return ecli;
    }

    public void setJustizMetadaten(RisJustizMetadaten justizMetadaten) {
        this.justizMetadaten = justizMetadaten;
    }

    public RisJustizMetadaten getJustizMetadaten() {
        return justizMetadaten;
    }
}
