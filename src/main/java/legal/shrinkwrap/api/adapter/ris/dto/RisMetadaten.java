package legal.shrinkwrap.api.adapter.ris.dto;

import at.gv.bka.ris.v26.soap.ws.client.TechnicalApplicationType;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDate;

@Getter
public class RisMetadaten {

    private String id;
    private TechnicalApplicationType applicationType;
    private String organ;

    private LocalDate published;
    private LocalDate changed;
    private String url;
    private String fullResponseAsJson;

    public RisMetadaten(String id, TechnicalApplicationType applicationType, String organ,LocalDate published, LocalDate changed, String url, String fullResponseAsJson) {
        this.id = id;
        this.applicationType = applicationType;
        this.organ = organ;
        this.published = published;
        this.changed = changed;
        this.url = url;
        this.fullResponseAsJson = fullResponseAsJson;
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
