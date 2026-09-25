package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * A body of law as the API reports it. Empty properties are left out rather than sent as null,
 * for the reason given at {@link NormProvisionDto}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NormLawDto(
        @JsonPropertyDescription("ISO 3166-2: AT for federal law, AT-1 … AT-9 for the states")
        String jurisdiction,
        @JsonPropertyDescription("RIS law number, the unambiguous way to address this law")
        String gesetzesnummer,
        @JsonProperty(required = false)
        String kurztitel,
        @JsonProperty(required = false)
        String langtitel,
        @JsonPropertyDescription("Every abbreviation and short title this law is known by")
        List<String> abkuerzungen,
        @JsonProperty(required = false)
        String typ,
        @JsonProperty(required = false)
        String kundmachungsorgan,
        @JsonProperty(required = false)
        String eli
) {
}
