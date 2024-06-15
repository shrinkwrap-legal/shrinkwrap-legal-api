package legal.shrinkwrap.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ServiceConfiguration.class)
public class TestServicesConfiguration {
}
