package legal.shrinkwrap.api.adapter.ris.dto;

import at.gv.bka.ris.v26.soap.ws.client.TechnicalApplicationType;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;

public class RisMetadaten {

    private String id;
    private TechnicalApplicationType applicationType;
    private String organ;

    private LocalDate published;
    private LocalDate changed;
    private String url;

    public RisMetadaten(String id, TechnicalApplicationType applicationType, String organ,LocalDate published, LocalDate changed, String url) {
        this.id = id;
        this.applicationType = applicationType;
        this.organ = organ;
        this.published = published;
        this.changed = changed;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public TechnicalApplicationType getApplicationType() {
        return applicationType;
    }

    public String getOrgan() {
        return organ;
    }

    public String getUrl() {
        return url;
    }

    public LocalDate getPublished() {
        return published;
    }

    public LocalDate getChanged() {
        return changed;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("applicationType", applicationType)
                .append("organ", organ)
                .append("published", published)
                .append("changed", changed)
                .append("url", url)
                .toString();
    }
}
