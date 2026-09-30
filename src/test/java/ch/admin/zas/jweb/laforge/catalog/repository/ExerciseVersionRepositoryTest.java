package ch.admin.zas.jweb.laforge.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.CodeFileKind;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.FeatureStatus;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ReviewVerdict;
import ch.admin.zas.jweb.laforge.catalog.domain.RubricImportance;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
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
 * Tests de persistance de {@link ExerciseVersionRepository}, le plus riche du référentiel : tous
 * les sous-documents {@code jsonb} ({@code learningObjectives}, {@code files}, {@code
 * technologies}, {@code responseSpec}, {@code hints}, {@code correction}) doivent survivre à un
 * aller-retour complet, ainsi que la relation {@code ManyToMany} vers {@link Topic}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class ExerciseVersionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExerciseVersionRepository exerciseVersionRepository;

    private Exercise persistExercise() {
        return entityManager.persistAndFlush(new Exercise());
    }

    private Topic persistTopic(String slug) {
        return entityManager.persistAndFlush(new Topic(slug, slug));
    }

    @Test
    void saveThenFindById_roundTripsAllJsonbFieldsAndTopics() {
        var exercise = persistExercise();
        var topic = persistTopic("java");
        var version = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, new LinkedHashSet<>(Set.of(topic)));

        var saved = exerciseVersionRepository.saveAndFlush(version);
        entityManager.clear();

        var reloaded = exerciseVersionRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getExercise().getId()).isEqualTo(exercise.getId());
        assertThat(reloaded.getVersionNumber()).isEqualTo(1);
        assertThat(reloaded.getType()).isEqualTo(ExerciseType.CODE_REVIEW);
        assertThat(reloaded.getDifficulty()).isEqualTo(Difficulty.INTERMEDIATE);
        assertThat(reloaded.getLearningObjectives()).containsExactly("Objectif 1", "Objectif 2");
        assertThat(reloaded.getFiles()).hasSize(1);
        assertThat(reloaded.getFiles().get(0).path()).isEqualTo("src/Main.java");
        assertThat(reloaded.getFiles().get(0).kind()).isEqualTo(CodeFileKind.SOURCE);
        assertThat(reloaded.getTechnologies()).hasSize(1);
        assertThat(reloaded.getTechnologies().get(0).featureStatus()).isEqualTo(FeatureStatus.STABLE);
        assertThat(reloaded.getResponseSpec().kind()).isEqualTo(ResponseKind.SINGLE_CHOICE);
        assertThat(reloaded.getResponseSpec().choices()).extracting("id").containsExactly("a", "b");
        assertThat(reloaded.getHints()).hasSize(2);
        assertThat(reloaded.getCorrection().correctChoiceIds()).containsExactly("a");
        assertThat(reloaded.getCorrection().expectedVerdicts()).containsExactly(ReviewVerdict.APPROVE);
        assertThat(reloaded.getCorrection().criteria().get(0).importance()).isEqualTo(RubricImportance.MAJOR);
        assertThat(reloaded.getCorrection().sources()).hasSize(1);
        assertThat(reloaded.getTopics()).extracting("id").containsExactly(topic.getId());
        assertThat(reloaded.isPublished()).isFalse();
    }

    @Test
    void findByExercise_IdAndVersionNumber_returnsMatchingVersion() {
        var exercise = persistExercise();
        var v1 = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of());
        var v2 = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 2, Set.of());
        exerciseVersionRepository.saveAllAndFlush(List.of(v1, v2));
        entityManager.clear();

        var found = exerciseVersionRepository.findByExercise_IdAndVersionNumber(exercise.getId(), 2);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getVersionNumber()).isEqualTo(2);
        assertThat(exerciseVersionRepository.findByExercise_IdAndVersionNumber(exercise.getId(), 99)).isEmpty();
    }

    @Test
    void findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc_returnsLatestPublished() {
        var exercise = persistExercise();
        var v1 = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of());
        v1.publish(OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        var v2 = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 2, Set.of());
        v2.publish(OffsetDateTime.parse("2024-02-01T00:00:00Z"));
        var v3Unpublished = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 3, Set.of());
        exerciseVersionRepository.saveAllAndFlush(List.of(v1, v2, v3Unpublished));
        entityManager.clear();

        var latestPublished =
                exerciseVersionRepository.findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(exercise.getId());

        assertThat(latestPublished).isPresent();
        assertThat(latestPublished.orElseThrow().getVersionNumber()).isEqualTo(2);
    }
}
