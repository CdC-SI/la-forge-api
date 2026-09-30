package ch.admin.zas.jweb.laforge.discovery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.domain.FeatureStatus;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.common.error.StaleVersionException;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.discovery.domain.Article;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link ArticleRepository} : round-trip des sous-documents {@code jsonb}
 * ({@code technologies}, {@code sources}), des relations {@code ManyToMany} (thèmes, versions
 * d'exercice liées) et du verrouillage optimiste porté par {@code revision}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class ArticleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ArticleRepository articleRepository;

    private Article sampleArticle(Set<Topic> topics, Set<ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion> versions) {
        return new Article(
                "Titre de la fiche",
                "Résumé",
                "Corps en *markdown*",
                ExerciseVersionFixtures.sampleTechnologies(),
                List.of(new Source("Source", "https://example.com", OffsetDateTime.parse("2024-01-01T00:00:00Z"))),
                topics,
                versions,
                OffsetDateTime.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void saveThenFindById_roundTripsJsonbFieldsAndRelations() {
        var topic = entityManager.persistAndFlush(new Topic("java", "Java"));
        var exercise = entityManager.persistAndFlush(new Exercise());
        var version = entityManager.persistAndFlush(ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of()));
        var article = sampleArticle(new LinkedHashSet<>(Set.of(topic)), new LinkedHashSet<>(Set.of(version)));

        var saved = articleRepository.saveAndFlush(article);
        entityManager.clear();

        var reloaded = articleRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Titre de la fiche");
        assertThat(reloaded.getTechnologies()).hasSize(1);
        assertThat(reloaded.getTechnologies().get(0).featureStatus()).isEqualTo(FeatureStatus.STABLE);
        assertThat(reloaded.getSources()).hasSize(1);
        assertThat(reloaded.getSources().get(0).title()).isEqualTo("Source");
        assertThat(reloaded.getTopics()).extracting("id").containsExactly(topic.getId());
        assertThat(reloaded.getRelatedExerciseVersions()).extracting("id").containsExactly(version.getId());
        assertThat(reloaded.getRevision()).isZero();
    }

    @Test
    void update_incrementsRevisionAndRejectsStaleExpectedRevision() {
        var article = articleRepository.saveAndFlush(sampleArticle(Set.of(), Set.of()));
        entityManager.clear();

        var managed = articleRepository.findById(article.getId()).orElseThrow();
        managed.update(0, "Nouveau titre", "Nouveau résumé", "Nouveau corps", List.of(), List.of(), Set.of(), Set.of());
        articleRepository.saveAndFlush(managed);
        entityManager.clear();

        var reloaded = articleRepository.findById(article.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Nouveau titre");
        assertThat(reloaded.getRevision()).isEqualTo(1);

        org.junit.jupiter.api.Assertions.assertThrows(StaleVersionException.class,
                () -> reloaded.update(0, "x", "x", "x", List.of(), List.of(), Set.of(), Set.of()));
    }
}
