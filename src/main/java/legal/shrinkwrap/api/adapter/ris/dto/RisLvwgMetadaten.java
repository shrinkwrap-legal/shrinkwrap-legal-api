package legal.shrinkwrap.api.adapter.ris.dto;

public class RisLvwgMetadaten extends RisBaseMetadaten {
    public RisLvwgMetadaten(String gericht, String entscheidungsart) {
        super(gericht);
        setEntscheidungsart(entscheidungsart);
    }
}
