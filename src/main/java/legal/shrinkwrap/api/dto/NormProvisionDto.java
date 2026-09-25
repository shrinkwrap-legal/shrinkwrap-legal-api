package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDate;

/**
 * One version of one provision.
 * <p>
 * Deliberately narrow. The document number is left out because it is the last segment of
 * {@link #risUrl}, which also serves as the filter to fetch exactly this version again; the kind
 * of provision because {@link #artikelParagraphAnlage} already reads "§ 50", "Art. 4 § 1" or
 * "Anl. 2/49"; and a separate "has text" flag because a {@link #wordCount} says the same thing.
 * <p>
 * An empty property is left out rather than sent as null. The MCP output schema types every
 * property without null, so a single null - an open end date is the normal case - would make the
 * SDK reject the whole answer; see {@code McpController}. That holds for the designation too: all
 * 678.219 documents carry one, but a single exception would take down an entire answer.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NormProvisionDto(
        @JsonPropertyDescription("Designation as RIS writes it: \"§ 50\", \"Art. 4 § 1\", \"Anl. 2/49\"")
        @JsonProperty(required = false)
        String artikelParagraphAnlage,
        @JsonPropertyDescription("A marker RIS sets on provisions consolidated from an amending act, "
                + "mostly at the end of a law: \"ÜR\" for transitional law, also \"EG/EU\", \"A\", \"S\", \"BVG\". "
                + "Such a provision names the amending act as its stammfassung")
        @JsonProperty(required = false)
        String uebergangsrecht,
        @JsonPropertyDescription("Link to this version of the provision in the RIS interface; pass it "
                + "back as risUrl to fetch exactly this version, e.g. with its text")
        @JsonProperty(required = false)
        String risUrl,
        @JsonProperty(required = false)
        LocalDate inkrafttreten,
        @JsonPropertyDescription("Frequently absent - a provision in force has no end date")
        @JsonProperty(required = false)
        LocalDate ausserkrafttreten,
        @JsonPropertyDescription("Where the provision was originally enacted, e.g. \"JGS Nr. 946/1811\"")
        @JsonProperty(required = false)
        String stammfassung,
        @JsonPropertyDescription("Which publication last touched it, e.g. \"BGBl. I Nr. 59/2017\". "
                + "Together with ausserkrafttreten this says whether it amended or repealed")
        @JsonProperty(required = false)
        String letzteAenderung,
        @JsonProperty(required = false)
        Long wordCount,
        @JsonPropertyDescription("Only present when the text was requested")
        @JsonProperty(required = false)
        String fullText,
        @JsonPropertyDescription("Images and annex files; only present together with the text, "
                + "and their urls appear inside the text as well")
        @JsonProperty(required = false)
        String attachments
) {
}
