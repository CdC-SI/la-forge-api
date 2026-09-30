package ch.admin.zas.jweb.laforge.authoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.EditorialReview;
import ch.admin.zas.jweb.laforge.authoring.domain.ReviewDecision;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
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
 * Tests de persistance de {@link EditorialReviewRepository} : round-trip d'une relecture
 * (trace d'audit immuable) et requêtes dérivées.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class EditorialReviewRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EditorialReviewRepository editorialReviewRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Someone"));
    }

    private Draft persistDraft(Account author) {
        var exercise = entityManager.persistAndFlush(new Exercise());
        var draft = new Draft(
                exercise,
                author,
                0,
                "Titre",
                ExerciseType.CODE_REVIEW,
                Difficulty.INTERMEDIATE,
                20,
                "Énoncé",
                List.of("Objectif"),
                ExerciseVersionFixtures.sampleFiles(),
                ExerciseVersionFixtures.sampleTechnologies(),
                ExerciseVersionFixtures.sampleResponseSpec(),
                ExerciseVersionFixtures.sampleHints(),
                ExerciseVersionFixtures.sampleCorrection(),
                Set.of());
        return entityManager.persistAndFlush(draft);
    }

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var author = persistAccount("author@example.com");
        var reviewer = persistAccount("reviewer@example.com");
        var draft = persistDraft(author);
        var reviewedAt = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        var review = new EditorialReview(draft, reviewer, ReviewDecision.REQUEST_CHANGES, "Manque des tests", reviewedAt);

        var saved = editorialReviewRepository.saveAndFlush(review);
        entityManager.clear();

        var reloaded = editorialReviewRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDraft().getId()).isEqualTo(draft.getId());
        assertThat(reloaded.getReviewer().getId()).isEqualTo(reviewer.getId());
        assertThat(reloaded.getDecision()).isEqualTo(ReviewDecision.REQUEST_CHANGES);
        assertThat(reloaded.getComment()).isEqualTo("Manque des tests");
        assertThat(reloaded.getReviewedAt()).isEqualTo(reviewedAt);
    }

    @Test
    void findByDraftOrderByReviewedAtAsc_ordersChronologically() {
        var author = persistAccount("author2@example.com");
        var reviewer = persistAccount("reviewer2@example.com");
        var draft = persistDraft(author);
        editorialReviewRepository.saveAndFlush(new EditorialReview(
                draft, reviewer, ReviewDecision.REQUEST_CHANGES, "Second passage", OffsetDateTime.parse("2024-02-01T00:00:00Z")));
        editorialReviewRepository.saveAndFlush(new EditorialReview(
                draft, reviewer, ReviewDecision.REQUEST_CHANGES, "Premier passage", OffsetDateTime.parse("2024-01-01T00:00:00Z")));
        entityManager.clear();

        var reloadedDraft = entityManager.find(Draft.class, draft.getId());
        var ordered = editorialReviewRepository.findByDraftOrderByReviewedAtAsc(reloadedDraft);

        assertThat(ordered).extracting(EditorialReview::getComment).containsExactly("Premier passage", "Second passage");
    }

    @Test
    void findDistinctDraftIdsByReviewer_returnsDistinctDraftIds() {
        var author = persistAccount("author3@example.com");
        var reviewer = persistAccount("reviewer3@example.com");
        var draft = persistDraft(author);
        editorialReviewRepository.saveAndFlush(
                new EditorialReview(draft, reviewer, ReviewDecision.APPROVE, "Ok", OffsetDateTime.parse("2024-01-01T00:00:00Z")));
        editorialReviewRepository.saveAndFlush(new EditorialReview(
                draft, reviewer, ReviewDecision.REQUEST_CHANGES, "Encore un souci", OffsetDateTime.parse("2024-01-02T00:00:00Z")));

        var draftIds = editorialReviewRepository.findDistinctDraftIdsByReviewer(reviewer.getId());

        assertThat(draftIds).containsExactly(draft.getId());
    }
}
