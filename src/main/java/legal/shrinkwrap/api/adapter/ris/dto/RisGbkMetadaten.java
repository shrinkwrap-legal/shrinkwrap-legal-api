package legal.shrinkwrap.api.adapter.ris.dto;

public class RisGbkMetadaten extends RisBaseMetadaten {
    public RisGbkMetadaten(String kommission, String entscheidungsart) {
        super(kommission);
        setEntscheidungsart(entscheidungsart);
    }
}
