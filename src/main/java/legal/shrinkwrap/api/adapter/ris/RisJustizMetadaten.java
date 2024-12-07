package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.dto.AbstractRisGerichtMetadaten;

import java.util.List;

public class RisJustizMetadaten extends AbstractRisGerichtMetadaten {
    private String gericht;
    private String entscheidungsart;
    private String anmerkung;
    private String fundstelle;
    private List<String> rechtssatznummern;
    private List<String> rechtsgebiete;



    public RisJustizMetadaten(final String gericht, final String entscheidungsart, final String anmerkung, final String fundstelle,
                              final List<String> rechtssatznummern,
                              final List<String> rechtsgebiete) {
        this.gericht = gericht;
        this.entscheidungsart = entscheidungsart;
        this.anmerkung = anmerkung;
        this.fundstelle = fundstelle;
        this.rechtssatznummern = rechtssatznummern;
        this.rechtsgebiete = rechtsgebiete;
    }

    public String getGericht() {
        return gericht;
    }

    public String getAnmerkung() {
        return anmerkung;
    }

    public String getEntscheidungsart() {
        return entscheidungsart;
    }

    public String getFundstelle() {
        return fundstelle;
    }

    public List<String> getRechtssatznummern() {
        return rechtssatznummern;
    }

    public List<String> getRechtsgebiete() {
        return rechtsgebiete;
    }
}
