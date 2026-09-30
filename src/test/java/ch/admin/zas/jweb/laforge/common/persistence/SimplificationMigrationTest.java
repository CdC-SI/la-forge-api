package ch.admin.zas.jweb.laforge.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Migrations réelles dans un schéma PostgreSQL isolé, jamais dans le schéma applicatif. */
@EnabledIfEnvironmentVariable(named = "LAFORGE_MIGRATION_TEST_URL", matches = ".+")
class SimplificationMigrationTest {

    private String schema;
    private String url;
    private String user;
    private String password;

    @BeforeEach
    void setUp() {
        schema = "simplification_test_" + UUID.randomUUID().toString().replace("-", "");
        url = Objects.requireNonNull(System.getenv("LAFORGE_MIGRATION_TEST_URL"));
        user = Objects.requireNonNull(System.getenv("LAFORGE_MIGRATION_TEST_USER"));
        password = Objects.requireNonNull(System.getenv("LAFORGE_MIGRATION_TEST_PASSWORD"));
    }

    @AfterEach
    void removeTestSchema() {
        flyway(null).clean();
    }

    @Test
    void migratesAnEmptyDatabase() {
        var result = flyway(null).migrate();
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(12);
    }

    @Test
    void upgradesExistingDraftsAndRolesWithoutPublishingOrLosingHistory() throws SQLException {
        flyway("10").migrate();
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO account (id,email,password_hash,display_name,status,created_at,updated_at)
                    VALUES ('00000000-0000-0000-0000-000000000001','author@example.com','hash','Auteur','ACTIVE',now(),now()),
                           ('00000000-0000-0000-0000-000000000002','reviewer@example.com','hash','Relecteur','ACTIVE',now(),now());
                    INSERT INTO account_role VALUES
                        ('00000000-0000-0000-0000-000000000001','LEARNER'),
                        ('00000000-0000-0000-0000-000000000001','AUTHOR'),
                        ('00000000-0000-0000-0000-000000000001','REVIEWER'),
                        ('00000000-0000-0000-0000-000000000002','LEARNER'),
                        ('00000000-0000-0000-0000-000000000002','REVIEWER');
                    INSERT INTO exercise (id,created_at,updated_at)
                    VALUES ('10000000-0000-0000-0000-000000000001',now(),now()),
                           ('10000000-0000-0000-0000-000000000002',now(),now()),
                           ('10000000-0000-0000-0000-000000000003',now(),now());
                    INSERT INTO draft
                        (id,exercise_id,author_id,state,revision,base_version,published_version,title,type,difficulty,
                         estimated_minutes,prompt_markdown,learning_objectives,response_spec,correction,created_at,updated_at)
                    SELECT id,id,'00000000-0000-0000-0000-000000000001',
                           CASE WHEN id = '10000000-0000-0000-0000-000000000001' THEN 'IN_REVIEW'
                                WHEN id = '10000000-0000-0000-0000-000000000002' THEN 'APPROVED' ELSE 'PUBLISHED' END,
                           3,0,CASE WHEN id = '10000000-0000-0000-0000-000000000003' THEN 1 ELSE NULL END,
                           'Titre','QUIZ','BEGINNER',5,'Question','["Objectif"]','{}','{}',now(),now()
                    FROM exercise;
                    INSERT INTO editorial_review (id,draft_id,reviewer_id,decision,comment,reviewed_at,created_at,updated_at)
                    VALUES ('20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',
                            '00000000-0000-0000-0000-000000000002','APPROVE','Historique',now(),now(),now());
                    INSERT INTO exercise_version
                        (id,exercise_id,version_number,title,type,difficulty,estimated_minutes,prompt_markdown,
                         learning_objectives,response_spec,correction,published_at,created_at,updated_at)
                    VALUES ('30000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003',
                            1,'Publication','QUIZ','BEGINNER',5,'Question','["Objectif"]','{}','{}',now(),now(),now());
                    INSERT INTO attempt (id,learner_id,exercise_version_id,status,started_at,submitted_at,created_at,updated_at)
                    VALUES ('40000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001',
                            '30000000-0000-0000-0000-000000000001','SUBMITTED',now(),now(),now(),now());
                    """);
        }

        flyway(null).migrate();

        try (var connection = connection(); var statement = connection.createStatement()) {
            assertThat(count(connection, "SELECT count(*) FROM draft WHERE state = 'DRAFT' AND revision = 4")).isEqualTo(2);
            assertThat(count(connection, "SELECT count(*) FROM draft WHERE state = 'PUBLISHED' AND revision = 3")).isEqualTo(1);
            assertThat(count(connection, "SELECT count(*) FROM exercise_version")).isEqualTo(1);
            assertThat(count(connection, "SELECT count(*) FROM attempt WHERE status = 'SUBMITTED'")).isEqualTo(1);
            assertThat(count(connection, "SELECT count(*) FROM editorial_review WHERE comment = 'Historique'")).isEqualTo(1);
            assertThat(count(connection, "SELECT count(*) FROM account_role WHERE role = 'REVIEWER'")).isZero();
            assertThat(count(connection, "SELECT count(*) FROM account_role WHERE role = 'AUTHOR'")).isEqualTo(2);
            statement.execute("""
                    INSERT INTO exercise VALUES ('10000000-0000-0000-0000-000000000004',now(),now());
                    INSERT INTO draft
                        (id,exercise_id,author_id,state,revision,base_version,title,learning_objectives,created_at,updated_at)
                    VALUES ('10000000-0000-0000-0000-000000000004','10000000-0000-0000-0000-000000000004',
                            '00000000-0000-0000-0000-000000000001','DRAFT',0,0,'Titre seul','[]',now(),now());
                    """);
            assertThat(count(connection, "SELECT count(*) FROM draft WHERE title = 'Titre seul' AND correction IS NULL")).isEqualTo(1);
        }
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private Connection connection() throws SQLException {
        var connection = DriverManager.getConnection(url, user, password);
        connection.setSchema(schema);
        return connection;
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure().dataSource(url, user, password)
                .schemas(schema).defaultSchema(schema).cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }
}
