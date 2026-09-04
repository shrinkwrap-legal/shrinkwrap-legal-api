package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CaseLawSearchResponseDto {
    private List<CaseLawSearchResultDto> searchResults = new ArrayList<>();


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
        public final static class CaseLawSearchResultDto {
            @JsonPropertyDescription("Word count of the case law that was summarized")
            private @JsonProperty(value="wordCount", required = true) Long wordCount;

            @JsonPropertyDescription("AI generated summary of the case law. May be null if it was not yet generated")
            private CaselawSummaryCivilCase summary;
            private CaseLawMetadataDto metadata;
        }
}
