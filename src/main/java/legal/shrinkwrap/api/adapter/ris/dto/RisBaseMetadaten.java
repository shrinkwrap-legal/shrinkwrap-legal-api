package legal.shrinkwrap.api.adapter.ris.dto;

public abstract class RisBaseMetadaten {
    private String gericht;

    public RisBaseMetadaten(String gericht) {
        this.gericht = gericht;
    }
    public String getGericht() {
        return gericht;
    }

    public void setGericht(String gericht) {
        this.gericht = gericht;
    }
}
