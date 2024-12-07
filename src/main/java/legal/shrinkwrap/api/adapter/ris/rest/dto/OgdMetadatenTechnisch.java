package legal.shrinkwrap.api.adapter.ris.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OgdMetadatenTechnisch(@JsonProperty("ID")
                                    String id,
                                    @JsonProperty("Applikation")
                                    String applikation) {
}
