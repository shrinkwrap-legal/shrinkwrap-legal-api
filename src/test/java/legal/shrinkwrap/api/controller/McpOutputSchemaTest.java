package legal.shrinkwrap.api.controller;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import legal.shrinkwrap.api.dto.CaseLawFullTextDto;
import legal.shrinkwrap.api.dto.CaseLawMetadataDto;
import legal.shrinkwrap.api.dto.CaseLawSearchResponseDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.dto.NormLawDto;
import legal.shrinkwrap.api.dto.NormLawsDto;
import legal.shrinkwrap.api.dto.NormProvisionDto;
import legal.shrinkwrap.api.dto.NormProvisionsDto;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated output schema types every property without "null", so a single null value makes the
 * SDK reject the whole tool response - which is why {@link McpController} replaces the nulls before
 * answering. An AI summary regularly leaves properties empty, so this is the normal case, not an
 * edge case.
 * <p>
 * The test deliberately uses {@link McpJsonDefaults}, the mapper the MCP tool serialisation actually
 * runs through. Configuring a mapper of our own would not cover the real path.
 */
public class McpOutputSchemaTest {

    private final McpJsonMapper mapper = McpJsonDefaults.getMapper();

    @SuppressWarnings("unchecked")
    private JsonSchemaValidator.ValidationResponse validate(Class<?> type, Object instance) throws Exception {
        String schema = McpJsonSchemaGenerator.generateFromClass(type);
        String json = mapper.writeValueAsString(instance);
        return McpJsonDefaults.getSchemaValidator()
                .validate(mapper.readValue(schema, Map.class), mapper.readValue(json, Map.class));
    }

    /** Metadata as it comes from the RIS: a court and a decision date are always there. */
    private CaseLawMetadataDto metadata() {
        CaseLawMetadataDto metadata = new CaseLawMetadataDto();
        metadata.setCourt("OGH");
        metadata.setDecisionDate(new Date());
        return metadata;
    }

    /** A summary of which the AI only filled in the type of decision. */
    private CaselawSummaryCivilCase partialSummary() {
        CaselawSummaryCivilCase summary = new CaselawSummaryCivilCase();
        summary.setArt("Urteil");
        return summary;
    }

    private CaseLawSearchResponseDto searchResponse(CaselawSummaryCivilCase summary, CaseLawMetadataDto metadata) {
        CaseLawSearchResponseDto response = new CaseLawSearchResponseDto();
        response.setSearchResults(List.of(
                new CaseLawSearchResponseDto.CaseLawSearchResultDto(42L, summary, metadata)));
        return response;
    }

    @Test
    public void searchResultConformsAfterNullsWereReplaced() throws Exception {
        CaselawSummaryCivilCase summary = partialSummary();
        CaseLawMetadataDto metadata = metadata();
        McpController.replaceNulls(summary);
        McpController.replaceNulls(metadata);

        JsonSchemaValidator.ValidationResponse result = validate(
                CaseLawSearchResponseDto.class, searchResponse(summary, metadata));

        assertTrue(result.valid(), () -> "does not conform: " + result.errorMessage());
    }

    /** Without the replacement the same answer is rejected - this is what broke in production. */
    @Test
    public void searchResultWithNullsIsRejected() throws Exception {
        JsonSchemaValidator.ValidationResponse result = validate(
                CaseLawSearchResponseDto.class, searchResponse(partialSummary(), metadata()));

        assertFalse(result.valid(), "nulls have to be rejected, otherwise the replacement is pointless");
    }

    @Test
    public void fullTextConformsAfterNullsWereReplaced() throws Exception {
        CaseLawMetadataDto metadata = metadata();
        McpController.replaceNulls(metadata);
        CaseLawFullTextDto fullText = new CaseLawFullTextDto();
        fullText.setMetadata(metadata);
        fullText.setFullText("Der Oberste Gerichtshof hat ...");

        JsonSchemaValidator.ValidationResponse result = validate(CaseLawFullTextDto.class, fullText);

        assertTrue(result.valid(), () -> "does not conform: " + result.errorMessage());
    }

    /**
     * The norm answers leave empty properties out instead of replacing them. Real values of § 85
     * TKG 2021: in force, never amended, requested without text - five properties are empty, the
     * open end date among them, and that is the ordinary case.
     */
    @Test
    public void normProvisionsWithEmptyPropertiesConform() throws Exception {
        NormLawDto law = new NormLawDto("AT", "20011678", "Telekommunikationsgesetz 2021", null,
                List.of("TKG 2021"), "BG", "BGBl. I Nr. 190/2021 zuletzt geändert durch BGBl. I Nr. 94/2025", null);
        NormProvisionDto inForce = new NormProvisionDto("§ 85", null,
                "https://ris.bka.gv.at/eli/bgbl/i/2021/190/P85/NOR40238543", LocalDate.of(2021, 11, 1), null,
                "BGBl. I Nr. 190/2021", null, 650L, null, null);
        NormProvisionDto nothingButTheDesignation = new NormProvisionDto("§ 0", null, null, null, null,
                null, null, null, null, null);

        JsonSchemaValidator.ValidationResponse result = validate(NormProvisionsDto.class,
                new NormProvisionsDto(law, 2, List.of(inForce, nothingButTheDesignation)));

        assertTrue(result.valid(), () -> "does not conform: " + result.errorMessage());
    }

    @Test
    public void normProvisionsWithEveryPropertyConform() throws Exception {
        NormLawDto law = new NormLawDto("AT", "20002849", "Telekommunikationsgesetz 2003",
                "Bundesgesetz, mit dem ein Telekommunikationsgesetz erlassen wird<br/>StF: BGBl. I Nr. 70/2003",
                List.of("TKG 2003"), "BG", "BGBl. I Nr. 70/2003 aufgehoben durch BGBl. I Nr. 190/2021",
                "https://ris.bka.gv.at/eli/bgbl/i/2003/70/P0/NOR40000001");
        NormProvisionDto repealed = new NormProvisionDto("§ 1", "ÜR",
                "https://ris.bka.gv.at/eli/bgbl/i/2003/70/P1/NOR40000002", LocalDate.of(2003, 8, 20),
                LocalDate.of(2021, 10, 31), "BGBl. I Nr. 70/2003", "BGBl. I Nr. 190/2021", 120L,
                "§ 1. (1) Zweck dieses Bundesgesetzes ist …", "[]");

        JsonSchemaValidator.ValidationResponse result = validate(NormProvisionsDto.class,
                new NormProvisionsDto(law, 1, List.of(repealed)));

        assertTrue(result.valid(), () -> "does not conform: " + result.errorMessage());
    }

    @Test
    public void normLawsWithEmptyPropertiesConform() throws Exception {
        NormLawDto law = new NormLawDto("AT", "10012137", "Kesselgesetz", null, List.of(), null, null, null);

        JsonSchemaValidator.ValidationResponse result = validate(NormLawsDto.class,
                new NormLawsDto(1, List.of(law)));

        assertTrue(result.valid(), () -> "does not conform: " + result.errorMessage());
    }
}
