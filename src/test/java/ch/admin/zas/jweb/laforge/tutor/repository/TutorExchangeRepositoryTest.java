package ch.admin.zas.jweb.laforge.tutor.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult;
import ch.admin.zas.jweb.laforge.practice.domain.SingleChoiceAnswer;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.tutor.domain.TutorExchange;
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
 * Tests de persistance de {@link TutorExchangeRepository} : round-trip du sous-document {@code
 * jsonb} {@code sources} et ordre chronologique des échanges d'une même tentative.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class TutorExchangeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TutorExchangeRepository tutorExchangeRepository;

    private Attempt persistAttempt() {
        var learner = entityManager.persistAndFlush(new Account("tutor@example.com", "hash", "Learner"));
        var exercise = entityManager.persistAndFlush(new Exercise());
        var version = entityManager.persistAndFlush(ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of()));
        var attempt = new Attempt(learner, version, null, null, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        attempt.submit(new SingleChoiceAnswer("a", "r", 3), ObjectiveResult.CORRECT, OffsetDateTime.parse("2024-01-01T00:05:00Z"));
        return entityManager.persistAndFlush(attempt);
    }

    @Test
    void saveThenFindById_roundTripsSources() {
        var attempt = persistAttempt();
        var sources = List.of(new Source("Doc", "https://example.com/doc", OffsetDateTime.parse("2024-01-01T00:00:00Z")));
        var exchange = new TutorExchange(attempt, "Pourquoi ce choix ?", "Explication *markdown*", sources);

        var saved = tutorExchangeRepository.saveAndFlush(exchange);
        entityManager.clear();

        var reloaded = tutorExchangeRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAttempt().getId()).isEqualTo(attempt.getId());
        assertThat(reloaded.getQuestion()).isEqualTo("Pourquoi ce choix ?");
        assertThat(reloaded.getAnswerMarkdown()).isEqualTo("Explication *markdown*");
        assertThat(reloaded.getSources()).hasSize(1);
        assertThat(reloaded.getSources().get(0).title()).isEqualTo("Doc");
        assertThat(reloaded.isGeneratedByAi()).isTrue();
    }

    @Test
    void findByAttemptIdOrderByCreatedAtAscIdAsc_returnsChronologicalOrder() {
        var attempt = persistAttempt();
        var first = tutorExchangeRepository.saveAndFlush(new TutorExchange(attempt, "Q1", "A1", List.of()));
        var second = tutorExchangeRepository.saveAndFlush(new TutorExchange(attempt, "Q2", "A2", List.of()));
        entityManager.clear();

        var ordered = tutorExchangeRepository.findByAttemptIdOrderByCreatedAtAscIdAsc(attempt.getId());

        assertThat(ordered).extracting(TutorExchange::getQuestion).containsExactly("Q1", "Q2");
    }
}
