package legal.shrinkwrap.api.dto;

import java.util.List;

/** Table of contents of a law: the law itself plus its provisions, in the order RIS lists them. */
public record NormStructureDto(
        NormLawDto law,
        List<NormProvisionDto> provisions
) {
}
