package legal.shrinkwrap.api.adapter.ris.dto;

public abstract class AbstractRisResult {

    private RisMetadaten metadaten;
    private String htmlDocumentUrl;

    public AbstractRisResult(RisMetadaten metadaten, String htmlDocumentUrl) {
        this.metadaten = metadaten;
        this.htmlDocumentUrl = htmlDocumentUrl;
    }

    public RisMetadaten getMetadaten() {
        return metadaten;
    }

    public void setMetadaten(RisMetadaten metadaten) {
        this.metadaten = metadaten;
    }

    public String getHtmlDocumentUrl() {
        return htmlDocumentUrl;
    }

    public void setHtmlDocumentUrl(String htmlDocumentUrl) {
        this.htmlDocumentUrl = htmlDocumentUrl;
    }

}
