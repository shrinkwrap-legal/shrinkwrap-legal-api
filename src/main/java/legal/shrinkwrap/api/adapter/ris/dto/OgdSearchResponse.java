package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;


public record OgdSearchResponse(
        @JsonProperty(value = "OgdSearchResult")
        OgdSearchResult ogdSearchResult
) {
}
