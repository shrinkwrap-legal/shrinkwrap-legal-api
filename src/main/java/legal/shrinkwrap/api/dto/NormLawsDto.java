package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Laws found for a name, as a tool answers them. REST returns the bare list and the total in a
 * header; a tool result has to be an object, and a tool call does not see headers.
 */
public record NormLawsDto(
        @JsonPropertyDescription("Laws found before the limit was applied")
        int total,
        List<NormLawDto> laws
) {
}
