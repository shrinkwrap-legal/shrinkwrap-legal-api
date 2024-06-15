package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record CaseLawResponseDto(@JsonProperty("wordCount") Integer wordCount,
                                 String caselawHtml,
                                 Map<String, String> metadata) {
}
