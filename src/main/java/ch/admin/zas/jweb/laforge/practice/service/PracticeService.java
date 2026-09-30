package ch.admin.zas.jweb.laforge.practice.service;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeState;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeParticipantRepository;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeRepository;
import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.KeysetPredicates;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.practice.domain.Answer;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.domain.FreeTextAnswer;
import ch.admin.zas.jweb.laforge.practice.domain.MultipleChoiceAnswer;
import ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult;
import ch.admin.zas.jweb.laforge.practice.domain.ReviewAnswer;
import ch.admin.zas.jweb.laforge.practice.domain.SelfAssessmentMastery;
import ch.admin.zas.jweb.laforge.practice.domain.SingleChoiceAnswer;
import ch.admin.zas.jweb.laforge.practice.dto.AttemptDto;
import ch.admin.zas.jweb.laforge.practice.dto.CreateAttemptInput;
import ch.admin.zas.jweb.laforge.practice.dto.DebriefDto;
import ch.admin.zas.jweb.laforge.practice.dto.SelfAssessmentDto;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.review.domain.ReviewItem;
import ch.admin.zas.jweb.laforge.review.domain.ReviewReason;
import ch.admin.zas.jweb.laforge.review.domain.ReviewState;
import ch.admin.zas.jweb.laforge.review.dto.ReviewItemDto;
import ch.admin.zas.jweb.laforge.review.repository.ReviewItemRepository;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycle de vie complet d'une tentative de pratique : création (individuelle, défi ou révision),
 * soumission avec correction automatique déterministe, indices, débrief et autoévaluation.
 * Aucune dépendance à l'IA (voir domaine {@code tutor} séparé).
 */
@Service
@Transactional(readOnly = true)
public class PracticeService {

    private final AttemptRepository attemptRepository;
    private final ExerciseVersionRepository exerciseVersionRepository;
    private final ReviewItemRepository reviewItemRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final AccountRepository accountRepository;
    private final Clock clock;

    public PracticeService(
            AttemptRepository attemptRepository,
            ExerciseVersionRepository exerciseVersionRepository,
            ReviewItemRepository reviewItemRepository,
            ChallengeRepository challengeRepository,
            ChallengeParticipantRepository challengeParticipantRepository,
            AccountRepository accountRepository,
            Clock clock) {
        this.attemptRepository = attemptRepository;
        this.exerciseVersionRepository = exerciseVersionRepository;
        this.reviewItemRepository = reviewItemRepository;
        this.challengeRepository = challengeRepository;
        this.challengeParticipantRepository = challengeParticipantRepository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    /**
     * Démarre une tentative individuelle, de défi ou de révision.
     *
     * @throws BadRequestException    si {@code challengeId} et {@code reviewItemId} sont tous deux fournis
     * @throws NotFoundException      si l'exercice/version, le défi ou la révision n'existe pas ou est hors périmètre
     * @throws InvalidStateException  si une précondition de défi, de révision ou de tentative en cours n'est pas respectée
     */
    @Transactional
    public AttemptDto createAttempt(CurrentAccountDto learner, CreateAttemptInput input) {
        if (input.challengeId() != null && input.reviewItemId() != null) {
            throw new BadRequestException("Une tentative ne peut être liée à la fois à un défi et à une révision.");
        }
        var version = exerciseVersionRepository
                .findByExercise_IdAndVersionNumber(input.exerciseId(), input.exerciseVersion())
                .filter(ExerciseVersion::isPublished)
                .orElseThrow(() -> new NotFoundException("Cette version d'exercice n'existe pas ou n'est pas publiée."));

        if (input.challengeId() != null) {
            validateChallengeAttempt(learner.id(), version, input.challengeId());
        } else if (input.reviewItemId() != null) {
            validateReviewAttempt(learner.id(), version, input.reviewItemId());
        } else if (attemptRepository.existsByLearner_IdAndExerciseVersion_IdAndStatus(
                learner.id(), version.getId(), AttemptStatus.IN_PROGRESS)) {
            throw new InvalidStateException("Une tentative est déjà en cours sur cette version d'exercice.");
        }

        var attempt = new Attempt(
                accountRepository.getReferenceById(learner.id()),
                version,
                input.challengeId(),
                input.reviewItemId(),
                OffsetDateTime.now(clock));
        return AttemptDto.from(attemptRepository.save(attempt));
    }

    private void validateChallengeAttempt(UUID learnerId, ExerciseVersion version, UUID challengeId) {
        var challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException("Défi introuvable."));
        if (challenge.getState() != ChallengeState.OPEN) {
            throw new InvalidStateException("Ce défi n'est plus ouvert.");
        }
        if (!challenge.getExerciseVersion().getId().equals(version.getId())) {
            throw new InvalidStateException("La version d'exercice ne correspond pas à ce défi.");
        }
        challengeParticipantRepository
                .findByChallenge_IdAndAccount_Id(challengeId, learnerId)
                .orElseThrow(() -> new InvalidStateException("Inscription au défi requise."));
        attemptRepository.findByChallengeIdAndLearner_Id(challengeId, learnerId)
                .ifPresent(existing -> {
                    throw new InvalidStateException("Une tentative existe déjà pour ce défi.");
                });
        if (attemptRepository.existsByLearner_IdAndExerciseVersion_IdAndStatus(
                learnerId, version.getId(), AttemptStatus.SUBMITTED)
                || attemptRepository.existsRevealedHintOutsideChallenge(learnerId, version.getId(), challengeId)) {
            throw new InvalidStateException(
                    "Une pratique antérieure de cette version hors de ce défi interdit d'y participer.");
        }
    }

    private void validateReviewAttempt(UUID learnerId, ExerciseVersion version, UUID reviewItemId) {
        var reviewItem = reviewItemRepository.findById(reviewItemId)
                .filter(item -> item.getLearner().getId().equals(learnerId))
                .orElseThrow(() -> new NotFoundException("Révision introuvable."));
        if (!reviewItem.getExerciseVersion().getId().equals(version.getId())) {
            throw new InvalidStateException("La version d'exercice ne correspond pas à cette révision.");
        }
        if (reviewItem.state(OffsetDateTime.now(clock)) != ReviewState.DUE) {
            throw new InvalidStateException("Cette révision n'est pas encore due ou est déjà terminée.");
        }
        attemptRepository.findByReviewItemId(reviewItemId)
                .filter(existing -> existing.getStatus() == AttemptStatus.IN_PROGRESS)
                .ifPresent(existing -> {
                    throw new InvalidStateException("Une tentative liée à cette révision est déjà en cours.");
                });
    }

    /**
     * Fige la réponse et déclenche la correction déterministe. Idempotent si la même réponse
     * canonique est déjà soumise ; une réponse différente sur une tentative déjà soumise est un conflit.
     *
     * @throws NotFoundException     si la tentative n'existe pas ou n'appartient pas à l'appelant
     * @throws InvalidStateException si la tentative n'est plus soumissible, ou si le défi lié est fermé
     * @throws ValidationFailedException si le type de réponse ne correspond pas au format attendu
     */
    @Transactional
    public AttemptDto submitAttempt(CurrentAccountDto learner, UUID attemptId, Answer answer) {
        var attempt = findOwnedAttempt(learner, attemptId);
        var version = attempt.getExerciseVersion();

        if (kindOf(answer) != version.getResponseSpec().kind()) {
            throw new ValidationFailedException("Le type de réponse ne correspond pas au format attendu.");
        }
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            if (attempt.getAnswer().equals(answer)) {
                return AttemptDto.from(attempt);
            }
            throw new InvalidStateException("Cette tentative a déjà été soumise avec une réponse différente.");
        }
        if (attempt.getChallengeId() != null) {
            challengeRepository.findById(attempt.getChallengeId())
                    .filter(challenge -> challenge.getState() != ChallengeState.OPEN)
                    .ifPresent(challenge -> {
                        throw new InvalidStateException("Le défi lié à cette tentative est fermé.");
                    });
        }

        var now = OffsetDateTime.now(clock);
        var result = grade(answer, version.getCorrection().correctChoiceIds());
        attempt.submit(answer, result, now);

        if (attempt.getReviewItemId() != null) {
            reviewItemRepository.findById(attempt.getReviewItemId()).ifPresent(item -> item.complete(now));
        }
        var triggersReview = result == ObjectiveResult.INCORRECT || answer.confidence() <= 2;
        if (triggersReview
                && reviewItemRepository
                        .findByLearner_IdAndExerciseIdAndCompletedAtIsNull(learner.id(), version.getExercise().getId())
                        .isEmpty()) {
            var reason = result == ObjectiveResult.INCORRECT ? ReviewReason.INCORRECT : ReviewReason.LOW_CONFIDENCE;
            var dueAt = now.plusDays(reason == ReviewReason.INCORRECT ? 1 : 2);
            reviewItemRepository.save(new ReviewItem(attempt.getLearner(), version, attempt, dueAt, reason));
        }
        return AttemptDto.from(attempt);
    }

    /** Correction déterministe : seuls les choix uniques/multiples sont notés automatiquement. */
    private static ObjectiveResult grade(Answer answer, List<String> correctChoiceIds) {
        return switch (answer) {
            case FreeTextAnswer ignored -> ObjectiveResult.NOT_AUTO_GRADED;
            case ReviewAnswer ignored -> ObjectiveResult.NOT_AUTO_GRADED;
            case SingleChoiceAnswer single -> Set.of(single.choiceId()).equals(Set.copyOf(correctChoiceIds))
                    ? ObjectiveResult.CORRECT
                    : ObjectiveResult.INCORRECT;
            case MultipleChoiceAnswer multiple -> Set.copyOf(multiple.choiceIds()).equals(Set.copyOf(correctChoiceIds))
                    ? ObjectiveResult.CORRECT
                    : ObjectiveResult.INCORRECT;
        };
    }

    private static ResponseKind kindOf(Answer answer) {
        return switch (answer) {
            case FreeTextAnswer ignored -> ResponseKind.FREE_TEXT;
            case SingleChoiceAnswer ignored -> ResponseKind.SINGLE_CHOICE;
            case MultipleChoiceAnswer ignored -> ResponseKind.MULTIPLE_CHOICE;
            case ReviewAnswer ignored -> ResponseKind.REVIEW;
        };
    }

    /**
     * Abandonne une tentative individuelle. Idempotent.
     *
     * @throws NotFoundException     si la tentative n'existe pas ou n'appartient pas à l'appelant
     * @throws InvalidStateException si la tentative est déjà soumise ou liée à un défi
     */
    @Transactional
    public AttemptDto abandonAttempt(CurrentAccountDto learner, UUID attemptId) {
        var attempt = findOwnedAttempt(learner, attemptId);
        attempt.abandon();
        return AttemptDto.from(attempt);
    }

    /**
     * Débloque un indice, dans l'ordre à partir du niveau 1.
     *
     * @throws NotFoundException         si la tentative n'existe pas ou n'appartient pas à l'appelant
     * @throws ValidationFailedException si le niveau demandé n'existe pas pour cet exercice
     * @throws InvalidStateException     si la tentative n'est pas en cours ou si le niveau saute un palier
     * @throws ch.admin.zas.jweb.laforge.common.error.ForbiddenException si la tentative est liée à un défi
     */
    @Transactional
    public Hint revealHint(CurrentAccountDto learner, UUID attemptId, int level) {
        var attempt = findOwnedAttempt(learner, attemptId);
        var hints = attempt.getExerciseVersion().getHints();
        if (level < 1 || level > hints.size()) {
            throw new ValidationFailedException("Ce niveau d'indice n'existe pas pour cet exercice.");
        }
        attempt.revealHint(level);
        return hints.get(level - 1);
    }

    /**
     * Corrigé et résultat figés à la soumission.
     *
     * @throws NotFoundException     si la tentative n'existe pas ou n'appartient pas à l'appelant
     * @throws InvalidStateException si la tentative n'est pas encore soumise
     */
    public DebriefDto getDebrief(CurrentAccountDto learner, UUID attemptId) {
        var attempt = findOwnedAttempt(learner, attemptId);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new InvalidStateException("Le débrief exige une tentative déjà soumise.");
        }
        var selfAssessment = attempt.getSelfAssessmentMastery() == null
                ? null
                : new SelfAssessmentDto(attempt.getSelfAssessmentMastery(), attempt.getSelfAssessmentNote());
        return new DebriefDto(
                attempt.getId(),
                attempt.getExerciseVersion().getVersionNumber(),
                attempt.getExerciseVersion().getCorrection(),
                attempt.getObjectiveResult(),
                attempt.getRevealedHintLevels().size(),
                selfAssessment);
    }

    /**
     * Enregistre ou remplace l'autoévaluation, en créant ou en replanifiant l'unique fiche de
     * révision de suivi issue de cette tentative. Répéter la même valeur ne décale pas l'échéance.
     *
     * @throws NotFoundException     si la tentative n'existe pas ou n'appartient pas à l'appelant
     * @throws InvalidStateException si la tentative n'est pas soumise, ou si la fiche de suivi est déjà commencée/terminée
     */
    @Transactional
    public ReviewItemDto setSelfAssessment(CurrentAccountDto learner, UUID attemptId, SelfAssessmentMastery mastery, String note) {
        var attempt = findOwnedAttempt(learner, attemptId);
        var previousMastery = attempt.getSelfAssessmentMastery();
        attempt.recordSelfAssessment(mastery, note);

        var now = OffsetDateTime.now(clock);
        var dueAt = now.plusDays(dayOffset(mastery));
        var existing = reviewItemRepository.findBySourceAttempt_Id(attemptId);
        if (existing.isEmpty()) {
            var created = reviewItemRepository.save(new ReviewItem(
                    attempt.getLearner(), attempt.getExerciseVersion(), attempt, dueAt, ReviewReason.SELF_ASSESSMENT));
            return ReviewItemDto.from(created, clock, attempt.getStatus() == AttemptStatus.SUBMITTED);
        }

        var reviewItem = existing.get();
        if (reviewItem.getCompletedAt() != null) {
            throw new InvalidStateException("Cette révision est déjà terminée et ne peut plus être replanifiée.");
        }
        attemptRepository.findByReviewItemId(reviewItem.getId())
                .filter(other -> !other.getId().equals(attemptId))
                .ifPresent(other -> {
                    throw new InvalidStateException("Cette révision est déjà commencée et ne peut plus être replanifiée.");
                });
        if (previousMastery != mastery) {
            reviewItem.reschedule(dueAt, ReviewReason.SELF_ASSESSMENT);
        }
        return ReviewItemDto.from(reviewItem, clock, attempt.getStatus() == AttemptStatus.SUBMITTED);
    }

    private static long dayOffset(SelfAssessmentMastery mastery) {
        return switch (mastery) {
            case AGAIN -> 1;
            case HARD -> 3;
            case GOOD -> 7;
            case EASY -> 21;
        };
    }

    /** @throws NotFoundException si la tentative n'existe pas ou n'appartient pas à l'appelant */
    public AttemptDto getAttempt(CurrentAccountDto learner, UUID attemptId) {
        return AttemptDto.from(findOwnedAttempt(learner, attemptId));
    }

    /** Tentatives de l'appelant, triées par {@code startedAt} décroissant puis id, filtrables par statut. */
    public Page<AttemptDto> listMyAttempts(CurrentAccountDto learner, PageQuery pageQuery, AttemptStatus status) {
        var filters = new HashMap<String, Object>();
        filters.put("status", status);
        var fingerprint = CursorCodec.fingerprint(filters);

        OffsetDateTime afterStartedAt = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterStartedAt = OffsetDateTime.parse(keys.get(0));
            afterId = UUID.fromString(keys.get(1));
        }

        Specification<Attempt> spec = (root, query, cb) -> cb.equal(root.get("learner").get("id"), learner.id());
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        spec = spec.and(KeysetPredicates.afterDescending("startedAt", afterStartedAt, afterId));

        var pageable = PageRequest.of(0, pageQuery.limit() + 1, Sort.by(Sort.Order.desc("startedAt"), Sort.Order.asc("id")));
        var rows = attemptRepository.findAll(spec, pageable).getContent();
        var hasMore = rows.size() > pageQuery.limit();
        var items = (hasMore ? rows.subList(0, pageQuery.limit()) : rows).stream().map(AttemptDto::from).toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getStartedAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    private Attempt findOwnedAttempt(CurrentAccountDto learner, UUID attemptId) {
        return attemptRepository.findByIdAndLearner_Id(attemptId, learner.id())
                .orElseThrow(() -> new NotFoundException("Tentative introuvable."));
    }
}
