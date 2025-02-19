package legal.shrinkwrap.api.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class YamlMapper {

    public static ObjectMapper getMapper() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
