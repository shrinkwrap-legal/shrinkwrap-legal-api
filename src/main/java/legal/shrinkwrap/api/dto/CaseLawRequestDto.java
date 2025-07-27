package legal.shrinkwrap.api.dto;

import jakarta.validation.constraints.Size;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;

public record CaseLawRequestDto(
        @Size(min = 1, max = 100) String ecli,
        @Size(max = 50) String docNumber,
        RisCourt court,
        Boolean includePrompts) {
}
