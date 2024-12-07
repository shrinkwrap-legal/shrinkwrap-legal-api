package legal.shrinkwrap.api.adapter.ris.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class RisJudikaturResult extends AbstractRisResult {

    private RisJudikaturMetadaten judikaturMetadaten;

    public RisJudikaturResult(RisMetadaten metadaten, RisJudikaturMetadaten judikaturMetadaten, String htmlDocumentUrl) {
        super(metadaten, htmlDocumentUrl);
        this.judikaturMetadaten = judikaturMetadaten;
    }

    public RisJudikaturMetadaten getJudikaturMetadaten() {
        return judikaturMetadaten;
    }

    public void setJudikaturMetadaten(RisJudikaturMetadaten judikaturMetadaten) {
        this.judikaturMetadaten = judikaturMetadaten;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("judikaturMetadaten", judikaturMetadaten)
                .toString();
    }
}
