package legal.shrinkwrap.api.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * flat structure of case law data
 */
public record CaseLawDataset (
        String metadataId,
        String metadataApplication,
        String metadataOrgan,
        @JsonFormat(locale = "en", shape = JsonFormat.Shape.STRING) LocalDate metadataPublished,
        @JsonFormat(locale = "en", shape = JsonFormat.Shape.STRING) LocalDate metadataChanged,
        String metadataUrl,
        String htmlUrl,
        String caseLawBusinessCode, //? "Geschäftszahl" -> Case number
        String caseLawEcli,
        String caseLawDocumentType,
        String justizGericht,
        String justizEntscheidungsart,
        String contentHtml,
        String sentences
        )
{


}
