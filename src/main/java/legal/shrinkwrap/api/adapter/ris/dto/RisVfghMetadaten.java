package legal.shrinkwrap.api.adapter.ris.dto;

public class RisVfghMetadaten extends RisBaseMetadaten {

    public RisVfghMetadaten(String gericht, String entscheidungsart) {
        super(gericht);
        setEntscheidungsart(entscheidungsart);
    }
}
