package legal.shrinkwrap.api.adapter.ris;

import io.soabase.recordbuilder.core.RecordBuilder;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;


@RecordBuilder
public record RisSearchParameterCaseLaw(
        RisCourt court,
        String ecli,
        JudikaturTyp judikaturTyp
) {
    public record JudikaturTyp(Boolean inRechtssaetzen, Boolean inEntscheidungstexten) {
    }
}
