package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** A body of law as the API reports it. */
public record NormLawDto(
        @JsonPropertyDescription("ISO 3166-2: AT for federal law, AT-1 … AT-9 for the states")
        String jurisdiction,
        @JsonPropertyDescription("RIS law number, the unambiguous way to address this law")
        String gesetzesnummer,
        String kurztitel,
        String langtitel,
        @JsonPropertyDescription("Every abbreviation and short title this law is known by")
        List<String> abkuerzungen,
        String typ,
        String kundmachungsorgan,
        String eli
) {
}
