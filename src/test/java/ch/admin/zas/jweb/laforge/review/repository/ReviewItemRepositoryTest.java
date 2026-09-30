package ch.admin.zas.jweb.laforge.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult;
import ch.admin.zas.jweb.laforge.practice.domain.SingleChoiceAnswer;
import ch.admin.zas.jweb.laforge.review.domain.ReviewItem;
import ch.admin.zas.jweb.laforge.review.domain.ReviewReason;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link ReviewItemRepository} : round-trip et requêtes utilisées par la
 * planification de révision espacée (fiche active par exercice, échéances dues).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class ReviewItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReviewItemRepository reviewItemRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    private ExerciseVersion persistExerciseVersion() {
        var exercise = entityManager.persistAndFlush(new Exercise());
        return entityManager.persistAndFlush(ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of()));
    }

    private Attempt persistSubmittedAttempt(Account learner, ExerciseVersion version) {
        var attempt = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        attempt.submit(new SingleChoiceAnswer("a", "r", 3), ObjectiveResult.INCORRECT, OffsetDateTime.parse("2024-01-01T00:05:00Z"));
        return entityManager.persistAndFlush(attempt);
    }

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var learner = persistAccount("review@example.com");
        var version = persistExerciseVersion();
        var attempt = persistSubmittedAttempt(learner, version);
        var dueAt = OffsetDateTime.parse("2024-01-05T00:00:00Z");
        var reviewItem = new ReviewItem(learner, version, attempt, dueAt, ReviewReason.INCORRECT);

        var saved = reviewItemRepository.saveAndFlush(reviewItem);
        entityManager.clear();

        var reloaded = reviewItemRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getLearner().getId()).isEqualTo(learner.getId());
        assertThat(reloaded.getExerciseVersion().getId()).isEqualTo(version.getId());
        assertThat(reloaded.getExerciseId()).isEqualTo(version.getExercise().getId());
        assertThat(reloaded.getSourceAttempt().getId()).isEqualTo(attempt.getId());
        assertThat(reloaded.getDueAt()).isEqualTo(dueAt);
        assertThat(reloaded.getReason()).isEqualTo(ReviewReason.INCORRECT);
        assertThat(reloaded.getCompletedAt()).isNull();
    }

    @Test
    void findByLearner_IdOrderByDueAtAscIdAsc_ordersByDueDate() {
        var learner = persistAccount("order@example.com");
        var version = persistExerciseVersion();
        var attempt1 = persistSubmittedAttempt(learner, version);
        var attempt2 = persistSubmittedAttempt(learner, version);
        var later = new ReviewItem(learner, version, attempt1, OffsetDateTime.parse("2024-02-01T00:00:00Z"), ReviewReason.INCORRECT);
        var earlier = new ReviewItem(learner, version, attempt2, OffsetDateTime.parse("2024-01-01T00:00:00Z"), ReviewReason.LOW_CONFIDENCE);
        reviewItemRepository.saveAllAndFlush(List.of(later, earlier));
        entityManager.clear();

        var ordered = reviewItemRepository.findByLearner_IdOrderByDueAtAscIdAsc(learner.getId());

        assertThat(ordered).extracting(ReviewItem::getDueAt)
                .containsExactly(OffsetDateTime.parse("2024-01-01T00:00:00Z"), OffsetDateTime.parse("2024-02-01T00:00:00Z"));
    }

    @Test
    void findByLearner_IdAndExerciseIdAndCompletedAtIsNull_ignoresCompletedItems() {
        var learner = persistAccount("active@example.com");
        var version = persistExerciseVersion();
        var attempt = persistSubmittedAttempt(learner, version);
        var reviewItem =
                new ReviewItem(learner, version, attempt, OffsetDateTime.parse("2024-01-05T00:00:00Z"), ReviewReason.INCORRECT);
        reviewItemRepository.saveAndFlush(reviewItem);
        entityManager.clear();

        var active = reviewItemRepository.findByLearner_IdAndExerciseIdAndCompletedAtIsNull(
                learner.getId(), version.getExercise().getId());
        assertThat(active).isPresent();

        var managed = reviewItemRepository.findById(active.orElseThrow().getId()).orElseThrow();
        managed.complete(OffsetDateTime.parse("2024-01-06T00:00:00Z"));
        reviewItemRepository.saveAndFlush(managed);
        entityManager.clear();

        assertThat(reviewItemRepository.findByLearner_IdAndExerciseIdAndCompletedAtIsNull(
                learner.getId(), version.getExercise().getId())).isEmpty();
    }

    @Test
    void findBySourceAttempt_Id_returnsAssociatedReviewItem() {
        var learner = persistAccount("source@example.com");
        var version = persistExerciseVersion();
        var attempt = persistSubmittedAttempt(learner, version);
        reviewItemRepository.saveAndFlush(
                new ReviewItem(learner, version, attempt, OffsetDateTime.parse("2024-01-05T00:00:00Z"), ReviewReason.INCORRECT));
        entityManager.clear();

        assertThat(reviewItemRepository.findBySourceAttempt_Id(attempt.getId())).isPresent();
    }

    @Test
    void countByLearner_IdAndCompletedAtIsNullAndDueAtLessThanEqual_countsDueItems() {
        var learner = persistAccount("due@example.com");
        var version = persistExerciseVersion();
        var attempt1 = persistSubmittedAttempt(learner, version);
        var attempt2 = persistSubmittedAttempt(learner, version);
        var due = new ReviewItem(learner, version, attempt1, OffsetDateTime.parse("2024-01-01T00:00:00Z"), ReviewReason.INCORRECT);
        var notYetDue =
                new ReviewItem(learner, version, attempt2, OffsetDateTime.parse("2024-06-01T00:00:00Z"), ReviewReason.LOW_CONFIDENCE);
        reviewItemRepository.saveAllAndFlush(List.of(due, notYetDue));

        var count = reviewItemRepository.countByLearner_IdAndCompletedAtIsNullAndDueAtLessThanEqual(
                learner.getId(), OffsetDateTime.parse("2024-03-01T00:00:00Z"));

        assertThat(count).isEqualTo(1);
    }
}
