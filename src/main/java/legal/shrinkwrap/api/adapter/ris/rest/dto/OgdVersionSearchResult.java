package legal.shrinkwrap.api.adapter.ris.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OgdVersionSearchResult(
    @JsonProperty(value = "Version")
    String version
) {


}