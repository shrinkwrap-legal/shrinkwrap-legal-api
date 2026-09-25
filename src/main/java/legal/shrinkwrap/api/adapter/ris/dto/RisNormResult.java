package legal.shrinkwrap.api.adapter.ris.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class RisNormResult extends AbstractRisResult {

    private RisNormMetadaten normMetadaten;

    /** Images and annex files; empty for the ordinary case of a text-only provision. */
    private List<RisNormAttachment> attachments;

    public RisNormResult(RisMetadaten metadaten, RisNormMetadaten normMetadaten, String htmlDocumentUrl,
                         List<RisNormAttachment> attachments) {
        super(metadaten, htmlDocumentUrl);
        this.normMetadaten = normMetadaten;
        this.attachments = attachments;
    }

    public RisNormMetadaten getNormMetadaten() {
        return normMetadaten;
    }

    public void setNormMetadaten(RisNormMetadaten normMetadaten) {
        this.normMetadaten = normMetadaten;
    }

    public List<RisNormAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<RisNormAttachment> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("normMetadaten", normMetadaten)
                .append("attachments", attachments == null ? 0 : attachments.size())
                .toString();
    }
}
