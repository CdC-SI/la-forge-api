package ch.admin.zas.jweb.laforge.practice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeState;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeParticipantRepository;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeRepository;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.domain.MultipleChoiceAnswer;
import ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult;
import ch.admin.zas.jweb.laforge.practice.domain.SelfAssessmentMastery;
import ch.admin.zas.jweb.laforge.practice.domain.SingleChoiceAnswer;
import ch.admin.zas.jweb.laforge.practice.dto.CreateAttemptInput;
import ch.admin.zas.jweb.laforge.review.domain.ReviewItem;
import ch.admin.zas.jweb.laforge.review.domain.ReviewReason;
import ch.admin.zas.jweb.laforge.review.repository.ReviewItemRepository;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests unitaires purs (Mockito) de {@link PracticeService}, en particulier l'algorithme de
 * planification de révision espacée. Aucun contexte Spring : les dépôts sont simulés et l'horloge
 * est figée pour rendre les échéances calculées déterministes.
 */
@ExtendWith(MockitoExtension.class)
class PracticeServiceTest {

    private static final Instant NOW_INSTANT = Instant.parse("2024-01-15T10:00:00Z");

    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private ExerciseVersionRepository exerciseVersionRepository;
    @Mock
    private ReviewItemRepository reviewItemRepository;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private ChallengeParticipantRepository challengeParticipantRepository;

    private final Clock clock = Clock.fixed(NOW_INSTANT, ZoneOffset.UTC);
    private PracticeService service;

    @BeforeEach
    void setUp() {
        service = new PracticeService(
                attemptRepository, exerciseVersionRepository, reviewItemRepository, challengeRepository,
                challengeParticipantRepository, clock);
    }

    private static void setId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    private static Account newAccount() {
        var account = new Account("learner@example.com", "hash", "Apprenant");
        setId(account, UUID.randomUUID());
        return account;
    }

    private static ExerciseVersion newSingleChoiceVersion(String correctChoiceId, List<Hint> hints) {
        var exercise = new Exercise();
        setId(exercise, UUID.randomUUID());
        var responseSpec = new ResponseSpec(ResponseKind.SINGLE_CHOICE, List.of());
        var correction = new Correction("Explication", null, List.of(correctChoiceId), null, null, null);
        var version = new ExerciseVersion(
                exercise, 1, "Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, 10, "Prompt",
                List.of("Objectif"), null, null, responseSpec, hints, correction, Set.of());
        setId(version, UUID.randomUUID());
        version.publish(OffsetDateTime.now(Clock.systemUTC()));
        return version;
    }

    private static Attempt newAttempt(Account learner, ExerciseVersion version, UUID challengeId, UUID reviewItemId, OffsetDateTime startedAt) {
        var attempt = new Attempt(learner, version, challengeId, reviewItemId, startedAt);
        setId(attempt, UUID.randomUUID());
        return attempt;
    }

    // --- submitAttempt --------------------------------------------------

    @Test
    void submitAttempt_reponseCorrecteChoixUnique_donneResultatCorrectSansRevision() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));

        var answer = new SingleChoiceAnswer("A", "raisonnement", 5);
        var dto = service.submitAttempt(learner, attempt.getId(), answer);

        assertThat(dto.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(attempt.getObjectiveResult()).isEqualTo(ObjectiveResult.CORRECT);
        verify(reviewItemRepository, never()).save(any());
    }

    @Test
    void submitAttempt_reponseIncorrecte_creeUneRevisionAUnJour() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));
        when(reviewItemRepository.findByLearner_IdAndExerciseIdAndCompletedAtIsNull(learner.getId(), version.getExercise().getId()))
                .thenReturn(Optional.empty());

        var answer = new SingleChoiceAnswer("B", "raisonnement", 5);
        var dto = service.submitAttempt(learner, attempt.getId(), answer);

        assertThat(dto.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(attempt.getObjectiveResult()).isEqualTo(ObjectiveResult.INCORRECT);
        var captor = ArgumentCaptor.forClass(ReviewItem.class);
        verify(reviewItemRepository).save(captor.capture());
        var savedItem = captor.getValue();
        assertThat(savedItem.getReason()).isEqualTo(ReviewReason.INCORRECT);
        assertThat(savedItem.getDueAt()).isEqualTo(OffsetDateTime.now(clock).plusDays(1));
    }

    @Test
    void submitAttempt_confianceFaibleMaisCorrecte_creeUneRevisionADeuxJours() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));
        when(reviewItemRepository.findByLearner_IdAndExerciseIdAndCompletedAtIsNull(learner.getId(), version.getExercise().getId()))
                .thenReturn(Optional.empty());

        var answer = new SingleChoiceAnswer("A", "raisonnement", 2);
        service.submitAttempt(learner, attempt.getId(), answer);

        assertThat(attempt.getObjectiveResult()).isEqualTo(ObjectiveResult.CORRECT);
        var captor = ArgumentCaptor.forClass(ReviewItem.class);
        verify(reviewItemRepository).save(captor.capture());
        var savedItem = captor.getValue();
        assertThat(savedItem.getReason()).isEqualTo(ReviewReason.LOW_CONFIDENCE);
        assertThat(savedItem.getDueAt()).isEqualTo(OffsetDateTime.now(clock).plusDays(2));
    }

    @Test
    void submitAttempt_memeReponseSurTentativeDejaSoumise_estIdempotent() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));

        var answer = new SingleChoiceAnswer("A", "raisonnement", 5);
        var firstDto = service.submitAttempt(learner, attempt.getId(), answer);
        var secondDto = service.submitAttempt(learner, attempt.getId(), answer);

        assertThat(secondDto).isEqualTo(firstDto);
        verify(reviewItemRepository, never()).save(any());
    }

    @Test
    void submitAttempt_reponseDifferenteSurTentativeDejaSoumise_estUnConflit() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));

        service.submitAttempt(learner, attempt.getId(), new SingleChoiceAnswer("A", "raisonnement", 5));

        assertThrows(InvalidStateException.class,
                () -> service.submitAttempt(learner, attempt.getId(), new SingleChoiceAnswer("B", "autre", 5)));
    }

    @Test
    void submitAttempt_typeDeReponseIncorrect_declencheUneValidationFailedException() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));

        var mismatchedAnswer = new MultipleChoiceAnswer(List.of("A"), "raisonnement", 5);
        assertThrows(ValidationFailedException.class, () -> service.submitAttempt(learner, attempt.getId(), mismatchedAnswer));
    }

    @Test
    void submitAttempt_avecRevisionLieeDejaCompletee_marqueLaRevisionCommeTerminee() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var reviewItemId = UUID.randomUUID();
        var attempt = newAttempt(learner, version, null, reviewItemId, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));
        var linkedReviewItem = mock(ReviewItem.class);
        when(reviewItemRepository.findById(reviewItemId)).thenReturn(Optional.of(linkedReviewItem));

        service.submitAttempt(learner, attempt.getId(), new SingleChoiceAnswer("A", "raisonnement", 5));

        verify(linkedReviewItem).complete(OffsetDateTime.now(clock));
    }

    // --- createAttempt ----------------------------------------------------

    @Test
    void createAttempt_avecDefiEtRevision_declencheBadRequestException() {
        var learner = newAccount();
        var input = new CreateAttemptInput(UUID.randomUUID(), 1, UUID.randomUUID(), UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> service.createAttempt(learner, input));
    }

    @Test
    void createAttempt_versionNonPublieeOuInexistante_declencheNotFoundException() {
        var learner = newAccount();
        var exerciseId = UUID.randomUUID();
        var input = new CreateAttemptInput(exerciseId, 1, null, null);
        when(exerciseVersionRepository.findByExercise_IdAndVersionNumber(exerciseId, 1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createAttempt(learner, input));
    }

    @Test
    void createAttempt_tentativeEnCoursSurMemeVersion_bloqueUneNouvelleTentative() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var input = new CreateAttemptInput(version.getExercise().getId(), version.getVersionNumber(), null, null);
        when(exerciseVersionRepository.findByExercise_IdAndVersionNumber(version.getExercise().getId(), version.getVersionNumber()))
                .thenReturn(Optional.of(version));
        when(attemptRepository.existsByLearner_IdAndExerciseVersion_IdAndStatus(learner.getId(), version.getId(), AttemptStatus.IN_PROGRESS))
                .thenReturn(true);

        assertThrows(InvalidStateException.class, () -> service.createAttempt(learner, input));
    }

    // --- setSelfAssessment -------------------------------------------------

    @Test
    void setSelfAssessment_again_planifieARevisionUnJourPlusTard() {
        assertDayOffset(SelfAssessmentMastery.AGAIN, 1);
    }

    @Test
    void setSelfAssessment_hard_planifieLaRevisionTroisJoursPlusTard() {
        assertDayOffset(SelfAssessmentMastery.HARD, 3);
    }

    @Test
    void setSelfAssessment_good_planifieLaRevisionSeptJoursPlusTard() {
        assertDayOffset(SelfAssessmentMastery.GOOD, 7);
    }

    @Test
    void setSelfAssessment_easy_planifieLaRevisionVingtEtUnJoursPlusTard() {
        assertDayOffset(SelfAssessmentMastery.EASY, 21);
    }

    private void assertDayOffset(SelfAssessmentMastery mastery, long expectedDays) {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusHours(1));
        attempt.submit(new SingleChoiceAnswer("A", "raisonnement", 5), ObjectiveResult.CORRECT, OffsetDateTime.now(clock));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));
        when(reviewItemRepository.findBySourceAttempt_Id(attempt.getId())).thenReturn(Optional.empty());
        when(reviewItemRepository.save(any(ReviewItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service.setSelfAssessment(learner, attempt.getId(), mastery, "note");

        assertThat(dto.dueAt()).isEqualTo(OffsetDateTime.now(clock).plusDays(expectedDays));
        assertThat(dto.reason()).isEqualTo(ReviewReason.SELF_ASSESSMENT);
    }

    @Test
    void setSelfAssessment_repeterLaMemeValeur_neReplanifiePasLaRevisionExistante() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusHours(1));
        attempt.submit(new SingleChoiceAnswer("A", "raisonnement", 5), ObjectiveResult.CORRECT, OffsetDateTime.now(clock));
        attempt.recordSelfAssessment(SelfAssessmentMastery.HARD, "premiere note");
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));

        var existingReviewItem = mock(ReviewItem.class);
        when(existingReviewItem.getId()).thenReturn(UUID.randomUUID());
        when(existingReviewItem.getCompletedAt()).thenReturn(null);
        when(existingReviewItem.getExerciseVersion()).thenReturn(version);
        when(existingReviewItem.getSourceAttempt()).thenReturn(attempt);
        when(reviewItemRepository.findBySourceAttempt_Id(attempt.getId())).thenReturn(Optional.of(existingReviewItem));
        when(attemptRepository.findByReviewItemId(existingReviewItem.getId())).thenReturn(Optional.of(attempt));

        service.setSelfAssessment(learner, attempt.getId(), SelfAssessmentMastery.HARD, "note repetee");

        verify(existingReviewItem, never()).reschedule(any(), any());
    }

    @Test
    void setSelfAssessment_changerLaValeur_replanifieLaRevisionExistante() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of());
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusHours(1));
        attempt.submit(new SingleChoiceAnswer("A", "raisonnement", 5), ObjectiveResult.CORRECT, OffsetDateTime.now(clock));
        attempt.recordSelfAssessment(SelfAssessmentMastery.HARD, "premiere note");
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));

        var existingReviewItem = mock(ReviewItem.class);
        when(existingReviewItem.getId()).thenReturn(UUID.randomUUID());
        when(existingReviewItem.getCompletedAt()).thenReturn(null);
        when(existingReviewItem.getExerciseVersion()).thenReturn(version);
        when(existingReviewItem.getSourceAttempt()).thenReturn(attempt);
        when(reviewItemRepository.findBySourceAttempt_Id(attempt.getId())).thenReturn(Optional.of(existingReviewItem));
        when(attemptRepository.findByReviewItemId(existingReviewItem.getId())).thenReturn(Optional.of(attempt));

        service.setSelfAssessment(learner, attempt.getId(), SelfAssessmentMastery.EASY, "nouvelle note");

        verify(existingReviewItem, times(1))
                .reschedule(OffsetDateTime.now(clock).plusDays(21), ReviewReason.SELF_ASSESSMENT);
    }

    // --- revealHint ---------------------------------------------------------

    @Test
    void revealHint_niveauHorsBornes_declencheValidationFailedException() {
        var learner = newAccount();
        var version = newSingleChoiceVersion("A", List.of(new Hint(1, "Indice 1")));
        var attempt = newAttempt(learner, version, null, null, OffsetDateTime.now(clock).minusMinutes(5));
        when(attemptRepository.findByIdAndLearner_Id(attempt.getId(), learner.getId())).thenReturn(Optional.of(attempt));

        assertThrows(ValidationFailedException.class, () -> service.revealHint(learner, attempt.getId(), 2));
    }
}
