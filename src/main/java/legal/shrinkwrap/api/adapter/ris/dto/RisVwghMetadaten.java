package legal.shrinkwrap.api.adapter.ris.dto;

public class RisVwghMetadaten extends RisBaseMetadaten {

    public RisVwghMetadaten(String gericht, String entscheidungsart) {
        super(gericht);
        setEntscheidungsart(entscheidungsart);
    }
}
