package legal.shrinkwrap.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseLawFullTextDto {
    public String fullText;
    public CaseLawMetadataDto metadata;
}
