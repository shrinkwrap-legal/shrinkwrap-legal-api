package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record CaseLawResponseDto(@JsonProperty("wordCount") Integer wordCount,
                                 @JsonIgnore String caselawHtml,
                                 Map<String, String> metadata) {
}
