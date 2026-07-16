package legal.shrinkwrap.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a Postgres container with the pgvector extension for tests.
 *
 * <p>The {@link ServiceConnection} annotation makes Spring Boot wire the
 * datasource (url/username/password) to this container automatically, so no
 * {@code spring.datasource.*} properties are needed in the test profile.
 *
 * <p>Import it into any test that needs a real database, e.g.
 * {@code @Import(TestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // pgvector image is Postgres-based but not named "postgres", so mark it compatible.
    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg17-bookworm")
                    .asCompatibleSubstituteFor("postgres");

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(PGVECTOR_IMAGE)
                // Enable the vector extension before Hibernate/Liquibase create the schema.
                .withInitScript("db/testcontainers/init-pgvector.sql");
    }
}