package ch.admin.zas.jweb.laforge.common.persistence;

import ch.admin.zas.jweb.laforge.catalog.domain.CodeFilesConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.CorrectionConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.HintsConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.LearningObjectivesConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpecConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirementsConverter;
import ch.admin.zas.jweb.laforge.discovery.domain.ArticleSourcesConverter;
import ch.admin.zas.jweb.laforge.practice.domain.AnswerConverter;
import ch.admin.zas.jweb.laforge.profile.domain.StackEntriesConverter;
import ch.admin.zas.jweb.laforge.tutor.domain.TutorSourcesConverter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Configuration partagée par les tests {@code @DataJpaTest} des dépôts. Une tranche {@code
 * @DataJpaTest} n'auto-configure ni Jackson (nécessaire aux {@code AttributeConverter} des
 * colonnes {@code jsonb}) ni l'audit JPA (nécessite un {@link Clock}) : on importe donc
 * explicitement {@link JpaAuditingConfig}, les convertisseurs JSON (composants Spring exclus du
 * scan restreint de la tranche) et l'auto-configuration Jackson pour obtenir un {@code
 * ObjectMapper} fonctionnel, plutôt que de réécrire les migrations Flyway pour H2 (le profil de
 * test utilise déjà {@code ddl-auto=create-drop} pour générer le schéma directement depuis les
 * entités, Flyway étant désactivé).
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({
        JpaAuditingConfig.class,
        HintsConverter.class,
        CorrectionConverter.class,
        CodeFilesConverter.class,
        ResponseSpecConverter.class,
        LearningObjectivesConverter.class,
        TechnologyRequirementsConverter.class,
        ArticleSourcesConverter.class,
        TutorSourcesConverter.class,
        AnswerConverter.class,
        StackEntriesConverter.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
public class RepositoryTestConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
