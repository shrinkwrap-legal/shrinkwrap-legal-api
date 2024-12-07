package legal.shrinkwrap.api.adapter.ris.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


public record OgdSearchResponse(
        @JsonProperty(value = "OgdSearchResult")
        OgdSearchResult ogdSearchResult
) {
}
