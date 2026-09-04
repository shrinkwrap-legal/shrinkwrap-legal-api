package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


import java.util.Date;

@Getter
@Setter
public class CaseLawMetadataDto {
    @JsonProperty(value = "decision_date", required = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date decisionDate;
    @JsonProperty(required = false)
    private String court;
    @JsonProperty(required = false)
    private String organ;

    @JsonProperty(value = "decision_type", required = false)
    private String decisionType;
    @JsonProperty(required = false)
    private String url;
    @JsonProperty(required = false)
    private String ecli;

    @JsonProperty(value = "case_number", required = false)
    private String caseNumber;
}
