package legal.shrinkwrap.api.config;

import legal.shrinkwrap.api.controller.NormController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(httpSecurityCorsConfigurer ->
                httpSecurityCorsConfigurer.configurationSource(request -> {
                    CorsConfiguration cors = new CorsConfiguration().applyPermitDefaultValues();
                    //a browser hides every response header it is not told about
                    cors.setExposedHeaders(List.of(NormController.TOTAL_COUNT));
                    return cors;
                })
        );

        http.csrf(csrf -> csrf.ignoringRequestMatchers(
                "/sse",
                "/mcp",
                "/mcp/message"
        ));

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/sse", "/mcp", "/mcp/message").permitAll()
                .anyRequest().permitAll()
        );
        return http.build();
    }
}
