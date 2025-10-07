package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CaseLawResponseDto {
    @JsonProperty("wordCount") Long wordCount;
    String summaryType = null;
    Integer analysisVersion = 1;
    CaselawSummaryCivilCase summary;
    CaseLawSummaryPromptsDto prompts;
    CaseLawMetadataDto metadata;
}

