package legal.shrinkwrap.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseLawSummaryPromptsDto {
    @JsonProperty("user_prompt")
    private String userPrompt;

    @JsonProperty("system_prompt")
    private String systemPrompt;
    private String model;
}
