package ch.admin.zas.jweb.laforge.security.repository;

import java.util.Objects;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Rejoue les transactions concurrentes du service sur PostgreSQL migré, dans un schéma isolé. */
@EnabledIfEnvironmentVariable(named = "LAFORGE_MIGRATION_TEST_URL", matches = ".+")
@DirtiesContext
class PostgresAccountRoleLockRepositoryTest extends AccountRoleLockRepositoryTest {
    private static final String SCHEMA = "admin_lock_test_" + UUID.randomUUID().toString().replace("-", "");
    private static Flyway flyway;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        var url = Objects.requireNonNull(System.getenv("LAFORGE_MIGRATION_TEST_URL"));
        var username = Objects.requireNonNull(System.getenv("LAFORGE_MIGRATION_TEST_USER"));
        var password = Objects.requireNonNull(System.getenv("LAFORGE_MIGRATION_TEST_PASSWORD"));
        flyway = Flyway.configure().dataSource(url, username, password)
                .schemas(SCHEMA).defaultSchema(SCHEMA).cleanDisabled(false).load();
        flyway.migrate();
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @AfterAll
    static void cleanIsolatedSchema() {
        if (flyway != null) {
            flyway.clean();
        }
    }
}
