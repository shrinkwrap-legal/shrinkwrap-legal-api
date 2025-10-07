package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


import java.util.Date;

@Getter
@Setter
public class CaseLawMetadataDto {
    @JsonProperty(value = "decision_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date decisionDate;
    private String court;
    private String organ;

    @JsonProperty("decision_type")
    private String decisionType;
    private String url;
    private String ecli;

    @JsonProperty("case_number")
    private String caseNumber;
}
