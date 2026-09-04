package legal.shrinkwrap.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class McpJsonConfiguration {

    /**
     * The generated MCP output schemas mark every property as required and type it without "null",
     * so a null value makes the SDK's server-side schema validation fail. Leaving nulls out of the
     * serialisation avoids that, together with the {@code required = false} on the optional
     * properties of the response DTOs.
     * <p>
     * The mapper is derived from the one Spring provides, so it keeps its configuration and follows
     * along on an upgrade. Only the MCP responses use it - the REST responses keep the unchanged
     * mapper and therefore their current shape, nulls included.
     */
    @Bean
    public McpJsonMapper mcpJsonMapper(JsonMapper jsonMapper) {
        return new JacksonMcpJsonMapper(jsonMapper.rebuild()
                .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build());
    }
}
