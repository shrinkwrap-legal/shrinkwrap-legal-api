package legal.shrinkwrap.api.adapter.ris.dto;

public class RisDskMetadaten extends RisBaseMetadaten {
    public RisDskMetadaten(String behoerde, String entscheidungsart) {
        super(behoerde);
        setEntscheidungsart(entscheidungsart);
    }
}
