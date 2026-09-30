package ch.admin.zas.jweb.laforge.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult;
import ch.admin.zas.jweb.laforge.practice.domain.SingleChoiceAnswer;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import ch.admin.zas.jweb.laforge.review.domain.ReviewItem;
import ch.admin.zas.jweb.laforge.review.domain.ReviewReason;
import ch.admin.zas.jweb.laforge.review.repository.ReviewItemRepository;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests unitaires légers de {@link ReviewService}. La construction fine de la {@link
 * Specification} par état n'est pas vérifiable sans dépôt réel (H2) ; ces tests se limitent donc
 * à confirmer que le filtre par défaut ({@code DUE}) ne lève pas d'exception et que la forme de
 * page retournée est cohérente lorsque le dépôt ne renvoie aucune ligne.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewItemRepository reviewItemRepository;
    @Mock
    private ExerciseCompletionService exerciseCompletionService;

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);

    private ReviewService newService() {
        return new ReviewService(reviewItemRepository, clock, exerciseCompletionService);
    }

    private static Account newLearner() {
        var account = new Account("learner@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    @Test
    void listMyReviews_stateAbsent_utiliseDueParDefautSansException() {
        when(reviewItemRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var page = newService().listMyReviews(CurrentAccountDto.from(newLearner()), new PageQuery(20, null), null);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listMyReviews_pageVide_neProduitPasDeCurseurSuivant() {
        when(reviewItemRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var page = newService().listMyReviews(
                CurrentAccountDto.from(newLearner()),
                new PageQuery(20, null),
                ch.admin.zas.jweb.laforge.review.domain.ReviewState.SCHEDULED);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listMyReviews_completeLesProjectionsEnUnLotPourLApprenantCourant() {
        var learner = newLearner();
        var exercise = new Exercise();
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        var version = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of());
        var now = OffsetDateTime.now(clock);
        var attempt = new Attempt(learner, version, null, null, now.minusDays(2));
        attempt.submit(new SingleChoiceAnswer("b", "raison", 3), ObjectiveResult.INCORRECT, now.minusDays(2).plusMinutes(1));
        var first = new ReviewItem(learner, version, attempt, now.minusDays(1), ReviewReason.INCORRECT);
        var second = new ReviewItem(learner, version, attempt, now, ReviewReason.SELF_ASSESSMENT);
        when(reviewItemRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second)));
        when(exerciseCompletionService.completedExerciseIds(learner.getId(), List.of(exercise.getId(), exercise.getId())))
                .thenReturn(Set.of(exercise.getId()));

        var page = newService().listMyReviews(CurrentAccountDto.from(learner), new PageQuery(20, null), null);

        assertThat(page.items()).extracting(item -> item.exercise().completed()).containsExactly(true, true);
        verify(exerciseCompletionService).completedExerciseIds(learner.getId(), List.of(exercise.getId(), exercise.getId()));
    }
}
