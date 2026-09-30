package ch.admin.zas.jweb.laforge.practice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult;
import ch.admin.zas.jweb.laforge.practice.domain.SingleChoiceAnswer;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link AttemptRepository} : round-trip de la réponse polymorphe {@code
 * jsonb} et des indices révélés ({@code @ElementCollection}), ainsi que les requêtes dérivées et
 * la projection agrégée {@link TopicProgressProjection} utilisée par {@code GET /me/progress}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class AttemptRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AttemptRepository attemptRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    private ExerciseVersion persistExerciseVersion(Set<Topic> topics) {
        var exercise = entityManager.persistAndFlush(new Exercise());
        var version = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, topics);
        return entityManager.persistAndFlush(version);
    }

    @Test
    void saveThenFindById_roundTripsAnswerAndRevealedHints() {
        var learner = persistAccount("attempt@example.com");
        var version = persistExerciseVersion(Set.of());
        var now = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        var attempt = new Attempt(learner, version, null, null, now);
        attempt.revealHint(1);
        attempt.submit(new SingleChoiceAnswer("a", "Parce que", 4), ObjectiveResult.CORRECT, now.plusMinutes(5));

        var saved = attemptRepository.saveAndFlush(attempt);
        entityManager.clear();

        var reloaded = attemptRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getLearner().getId()).isEqualTo(learner.getId());
        assertThat(reloaded.getExerciseVersion().getId()).isEqualTo(version.getId());
        assertThat(reloaded.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(reloaded.getRevealedHintLevels()).containsExactly(1);
        assertThat(reloaded.getAnswer()).isInstanceOf(SingleChoiceAnswer.class);
        assertThat(((SingleChoiceAnswer) reloaded.getAnswer()).choiceId()).isEqualTo("a");
        assertThat(reloaded.getObjectiveResult()).isEqualTo(ObjectiveResult.CORRECT);
    }

    @Test
    void findByLearner_IdOrderByStartedAtDescIdDesc_returnsMostRecentFirst() {
        var learner = persistAccount("order@example.com");
        var version = persistExerciseVersion(Set.of());
        var older = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        var newer = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-02-01T00:00:00Z"));
        attemptRepository.saveAllAndFlush(List.of(older, newer));
        entityManager.clear();

        var attempts = attemptRepository.findByLearner_IdOrderByStartedAtDescIdDesc(learner.getId());

        assertThat(attempts).extracting(Attempt::getStartedAt)
                .containsExactly(OffsetDateTime.parse("2024-02-01T00:00:00Z"), OffsetDateTime.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void findByIdAndLearner_Id_scopesToOwningLearner() {
        var learner = persistAccount("owner@example.com");
        var otherLearner = persistAccount("other@example.com");
        var version = persistExerciseVersion(Set.of());
        var attempt = attemptRepository.saveAndFlush(
                new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z")));
        entityManager.clear();

        assertThat(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).isPresent();
        assertThat(attemptRepository.findByIdAndLearner_Id(attempt.getId(), otherLearner.getId())).isEmpty();
    }

    @Test
    void existsByLearner_IdAndExerciseVersion_IdAndStatus_reflectsPersistedState() {
        var learner = persistAccount("exists@example.com");
        var version = persistExerciseVersion(Set.of());
        attemptRepository.saveAndFlush(new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z")));

        assertThat(attemptRepository.existsByLearner_IdAndExerciseVersion_IdAndStatus(
                learner.getId(), version.getId(), AttemptStatus.IN_PROGRESS)).isTrue();
        assertThat(attemptRepository.existsByLearner_IdAndExerciseVersion_IdAndStatus(
                learner.getId(), version.getId(), AttemptStatus.SUBMITTED)).isFalse();
    }

    @Test
    void findByReviewItemId_returnsAttemptLinkedToReview() {
        var learner = persistAccount("review-link@example.com");
        var version = persistExerciseVersion(Set.of());
        var reviewItemId = UUID.randomUUID();
        attemptRepository.saveAndFlush(
                new Attempt(learner, version, null, reviewItemId, OffsetDateTime.parse("2024-01-01T00:00:00Z")));
        entityManager.clear();

        assertThat(attemptRepository.findByReviewItemId(reviewItemId)).isPresent();
        assertThat(attemptRepository.findByReviewItemId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void challengeQueries_filterByChallengeIdAndLearner() {
        var learner = persistAccount("challenger@example.com");
        var version = persistExerciseVersion(Set.of());
        var challengeId = UUID.randomUUID();
        var attempt = new Attempt(learner, version, challengeId, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        attempt.submit(new SingleChoiceAnswer("a", "Parce que", 3), ObjectiveResult.CORRECT, OffsetDateTime.parse("2024-01-01T00:05:00Z"));
        attemptRepository.saveAndFlush(attempt);
        entityManager.clear();

        assertThat(attemptRepository.findByChallengeIdAndStatusOrderBySubmittedAtAscIdAsc(challengeId, AttemptStatus.SUBMITTED))
                .hasSize(1);
        assertThat(attemptRepository.findByChallengeIdAndLearner_Id(challengeId, learner.getId())).isPresent();
    }

    @Test
    void countByLearner_IdAndStatus_countsOnlyMatchingStatus() {
        var learner = persistAccount("counter@example.com");
        var version = persistExerciseVersion(Set.of());
        var inProgress = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        var submitted = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-02T00:00:00Z"));
        submitted.submit(new SingleChoiceAnswer("a", "r", 3), ObjectiveResult.CORRECT, OffsetDateTime.parse("2024-01-02T00:05:00Z"));
        attemptRepository.saveAllAndFlush(List.of(inProgress, submitted));

        assertThat(attemptRepository.countByLearner_IdAndStatus(learner.getId(), AttemptStatus.IN_PROGRESS)).isEqualTo(1);
        assertThat(attemptRepository.countByLearner_IdAndStatus(learner.getId(), AttemptStatus.SUBMITTED)).isEqualTo(1);
    }

    @Test
    void existsRevealedHintOutsideChallenge_detectsHintOutsideCurrentChallenge() {
        var learner = persistAccount("hint@example.com");
        var version = persistExerciseVersion(Set.of());
        var challengeId = UUID.randomUUID();
        var soloAttempt = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        soloAttempt.revealHint(1);
        attemptRepository.saveAndFlush(soloAttempt);

        assertThat(attemptRepository.existsRevealedHintOutsideChallenge(learner.getId(), version.getId(), challengeId)).isTrue();
        assertThat(attemptRepository.existsRevealedHintOutsideChallenge(learner.getId(), UUID.randomUUID(), challengeId)).isFalse();
    }

    @Test
    void findSubmittedAtSince_returnsOnlySubmittedAttemptsAfterThreshold() {
        var learner = persistAccount("since@example.com");
        var version = persistExerciseVersion(Set.of());
        var early = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        early.submit(new SingleChoiceAnswer("a", "r", 3), ObjectiveResult.CORRECT, OffsetDateTime.parse("2024-01-01T00:05:00Z"));
        var late = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-03-01T00:00:00Z"));
        late.submit(new SingleChoiceAnswer("a", "r", 3), ObjectiveResult.CORRECT, OffsetDateTime.parse("2024-03-01T00:05:00Z"));
        attemptRepository.saveAllAndFlush(List.of(early, late));

        var since = attemptRepository.findSubmittedAtSince(learner.getId(), OffsetDateTime.parse("2024-02-01T00:00:00Z"));

        assertThat(since).containsExactly(OffsetDateTime.parse("2024-03-01T00:05:00Z"));
    }

    @Test
    void findTopicProgress_aggregatesSubmittedAttemptsPerTopic() {
        var learner = persistAccount("progress@example.com");
        var topic = entityManager.persistAndFlush(new Topic("java", "Java"));
        var version = persistExerciseVersion(new LinkedHashSet<>(Set.of(topic)));
        var correct = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        correct.submit(new SingleChoiceAnswer("a", "r", 3), ObjectiveResult.CORRECT, OffsetDateTime.parse("2024-01-01T00:05:00Z"));
        var incorrect = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-02T00:00:00Z"));
        incorrect.submit(new SingleChoiceAnswer("b", "r", 3), ObjectiveResult.INCORRECT, OffsetDateTime.parse("2024-01-02T00:05:00Z"));
        attemptRepository.saveAllAndFlush(List.of(correct, incorrect));

        var progress = attemptRepository.findTopicProgress(learner.getId());

        assertThat(progress).hasSize(1);
        var topicProgress = progress.get(0);
        assertThat(topicProgress.getTopicId()).isEqualTo(topic.getId());
        assertThat(topicProgress.getSubmittedAttempts()).isEqualTo(2);
        assertThat(topicProgress.getCorrectChoiceAttempts()).isEqualTo(1);
        assertThat(topicProgress.getGradedChoiceAttempts()).isEqualTo(2);
        assertThat(topicProgress.getLastPracticedAt()).isEqualTo(OffsetDateTime.parse("2024-01-02T00:05:00Z"));
    }
}
