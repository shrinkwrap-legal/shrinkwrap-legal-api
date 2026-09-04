package legal.shrinkwrap.api.controller;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import legal.shrinkwrap.api.dto.CaseLawFullTextDto;
import legal.shrinkwrap.api.dto.CaseLawMetadataDto;
import legal.shrinkwrap.api.dto.CaseLawSearchResponseDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator;

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
}
