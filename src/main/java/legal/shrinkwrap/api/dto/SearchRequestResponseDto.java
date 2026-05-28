package legal.shrinkwrap.api.dto;

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequestResponseDto {

    @Setter
    @Getter
    public static class SingleSearchRequestResponseDto {
        String docnumber;
        RisCourt court;
        CaselawSummaryCivilCase aiSummary;
    }
}
