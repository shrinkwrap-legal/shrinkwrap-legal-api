package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OgdVersionResponse(
        @JsonProperty(value = "OgdSearchResult")
        OgdVersionSearchResult ogdSearchResult
) {

}