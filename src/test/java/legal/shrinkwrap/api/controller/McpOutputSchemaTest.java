package legal.shrinkwrap.api.controller;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import legal.shrinkwrap.api.config.McpJsonConfiguration;
import legal.shrinkwrap.api.dto.CaseLawFullTextDto;
import legal.shrinkwrap.api.dto.CaseLawMetadataDto;
import legal.shrinkwrap.api.dto.CaseLawSearchResponseDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.method.tool.utils.McpJsonSchemaGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The MCP output schemas are generated with every property marked as required and typed without
 * "null", so an unset property makes the SDK's server-side validation reject the whole response.
 * The response DTOs therefore mark their optional properties as not required, and
 * {@link McpJsonConfiguration} leaves nulls out of the serialisation.
 * <p>
 * These tests answer with an almost empty result - what a case without an AI summary looks like -
 * and check that it still passes validation.
 */
public class McpOutputSchemaTest {

    private final McpJsonMapper mapper = new McpJsonConfiguration().mcpJsonMapper(JsonMapper.builder().build());

    @SuppressWarnings("unchecked")
    private void assertConformsToSchema(Class<?> type, Object instance) throws Exception {
        String schema = McpJsonSchemaGenerator.generateFromClass(type);
        String json = mapper.writeValueAsString(instance);

        JsonSchemaValidator.ValidationResponse result = McpJsonDefaults.getSchemaValidator()
                .validate(mapper.readValue(schema, Map.class), mapper.readValue(json, Map.class));

        assertTrue(result.valid(), () -> type.getSimpleName() + " does not conform: "
                + result.errorMessage() + " - serialised as " + json);
    }

    @Test
    public void searchResultWithoutSummaryConformsToSchema() throws Exception {
        CaseLawSearchResponseDto response = new CaseLawSearchResponseDto();
        response.setSearchResults(List.of(
                new CaseLawSearchResponseDto.CaseLawSearchResultDto(42L, null, new CaseLawMetadataDto())));

        assertConformsToSchema(CaseLawSearchResponseDto.class, response);
    }

    @Test
    public void searchResultWithEmptySummaryConformsToSchema() throws Exception {
        CaselawSummaryCivilCase summary = new CaselawSummaryCivilCase();
        summary.setArt(""); //the only property the schema still requires

        CaseLawSearchResponseDto response = new CaseLawSearchResponseDto();
        response.setSearchResults(List.of(
                new CaseLawSearchResponseDto.CaseLawSearchResultDto(42L, summary, new CaseLawMetadataDto())));

        assertConformsToSchema(CaseLawSearchResponseDto.class, response);
    }

    /**
     * A case whose text conversion has not run yet: the service always sets the metadata, but
     * leaves the full text unset.
     */
    @Test
    public void fullTextWithoutTextConformsToSchema() throws Exception {
        CaseLawFullTextDto fullText = new CaseLawFullTextDto();
        fullText.setMetadata(new CaseLawMetadataDto());

        assertConformsToSchema(CaseLawFullTextDto.class, fullText);
    }
}
