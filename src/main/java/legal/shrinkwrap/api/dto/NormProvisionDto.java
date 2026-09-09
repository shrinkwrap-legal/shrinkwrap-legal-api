package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDate;

/**
 * One version of one provision. {@code hasText} and {@code wordCount} are filled even when the
 * text itself is not requested, so a caller can see what a full retrieval would cost.
 */
public record NormProvisionDto(
        String docNumber,
        String abschnittTyp,
        @JsonPropertyDescription("Designation as RIS writes it: \"§ 50\", \"Art. 4 § 1\", \"Anl. 2/49\"")
        String artikelParagraphAnlage,
        @JsonPropertyDescription("Link to this version of the provision in the RIS interface")
        String risUrl,
        LocalDate inkrafttreten,
        @JsonPropertyDescription("Frequently absent - a provision in force has no end date")
        LocalDate ausserkrafttreten,
        boolean hasText,
        Long wordCount,
        @JsonPropertyDescription("Only present when the text was requested")
        String fullText,
        @JsonPropertyDescription("Images and annex files; only present together with the text, "
                + "and their urls appear inside the text as well")
        String attachments
) {
}
