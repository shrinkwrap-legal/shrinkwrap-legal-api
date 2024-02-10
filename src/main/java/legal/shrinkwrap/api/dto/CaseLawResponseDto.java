package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CaseLawResponseDto(@JsonProperty("wordCount") Integer wordCount) {
}
