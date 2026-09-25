package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Provisions together with the law they belong to. The law is part of the answer because a
 * caller addressing it by abbreviation never sees which law the abbreviation resolved to, and
 * the total because a tool call does not see the X-Total-Count header.
 */
public record NormProvisionsDto(
        NormLawDto law,
        @JsonPropertyDescription("Provisions found before the limit was applied; more than the list "
                + "holds means the list was cut off")
        int total,
        List<NormProvisionDto> provisions
) {
}
