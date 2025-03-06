package legal.shrinkwrap.api.adapter.ris.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class RisBaseMetadaten {
    private String gericht;
    private String entscheidungsart;

    public RisBaseMetadaten(String gericht) {
        this.gericht = gericht;
    }

    public RisBaseMetadaten(String gericht, String entscheidungsart) {
        this.gericht = gericht;
        this.entscheidungsart = entscheidungsart;
    }
}
