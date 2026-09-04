package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseLawFullTextDto {
    @JsonPropertyDescription("Metadata about the specific case")
    public CaseLawMetadataDto metadata;

    @JsonPropertyDescription("The full text of the case law document, text only, not including markup.")
    public String fullText;
}
