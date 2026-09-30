package ch.admin.zas.jweb.laforge.collective.service;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeParticipant;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeState;
import ch.admin.zas.jweb.laforge.collective.domain.DiscussionComment;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeCreatedDto;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeDto;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeInput;
import ch.admin.zas.jweb.laforge.collective.dto.DiscussionCommentDto;
import ch.admin.zas.jweb.laforge.collective.dto.SharedResponseDto;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeParticipantRepository;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeRepository;
import ch.admin.zas.jweb.laforge.collective.repository.DiscussionCommentRepository;
import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.KeysetPredicates;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Défis collectifs : création, adhésion par code, comparaison des réponses soumises et débrief
 * discuté. Le code d'invitation n'est jamais stocké en clair (voir {@link SecureTokenFactory}).
 */
@Service
@Transactional(readOnly = true)
public class CollectiveService {

    private static final int MAX_CLOSES_AT_DAYS = 30;

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ExerciseVersionRepository exerciseVersionRepository;
    private final AttemptRepository attemptRepository;
    private final DiscussionCommentRepository discussionCommentRepository;
    private final AccountRepository accountRepository;
    private final SecureTokenFactory secureTokenFactory;
    private final Clock clock;

    public CollectiveService(
            ChallengeRepository challengeRepository,
            ChallengeParticipantRepository challengeParticipantRepository,
            ExerciseVersionRepository exerciseVersionRepository,
            AttemptRepository attemptRepository,
            DiscussionCommentRepository discussionCommentRepository,
            AccountRepository accountRepository,
            SecureTokenFactory secureTokenFactory,
            Clock clock) {
        this.challengeRepository = challengeRepository;
        this.challengeParticipantRepository = challengeParticipantRepository;
        this.exerciseVersionRepository = exerciseVersionRepository;
        this.attemptRepository = attemptRepository;
        this.discussionCommentRepository = discussionCommentRepository;
        this.accountRepository = accountRepository;
        this.secureTokenFactory = secureTokenFactory;
        this.clock = clock;
    }

    /**
     * Crée un défi ouvert immédiatement ; le créateur est inscrit automatiquement.
     *
     * @throws BadRequestException si l'échéance n'est pas strictement future ou dépasse 30 jours
     * @throws NotFoundException  si l'exercice/version n'existe pas ou n'est pas publié
     */
    @Transactional
    public ChallengeCreatedDto createChallenge(CurrentAccountDto currentAccount, ChallengeInput input) {
        var now = OffsetDateTime.now(clock);
        if (!input.closesAt().isAfter(now) || input.closesAt().isAfter(now.plusDays(MAX_CLOSES_AT_DAYS))) {
            throw new BadRequestException("L'échéance doit être strictement future et au maximum dans 30 jours.");
        }
        var version = exerciseVersionRepository
                .findByExercise_IdAndVersionNumber(input.exerciseId(), input.exerciseVersion())
                .filter(ExerciseVersion::isPublished)
                .orElseThrow(() -> new NotFoundException("Cette version d'exercice n'existe pas ou n'est pas publiée."));

        var joinCode = secureTokenFactory.generateOpaqueToken();
        var creator = accountRepository.getReferenceById(currentAccount.id());
        var challenge = new Challenge(input.title(), version, creator, input.closesAt(), secureTokenFactory.hash(joinCode));
        challenge = challengeRepository.save(challenge);
        challengeParticipantRepository.save(new ChallengeParticipant(challenge, creator, now));

        return new ChallengeCreatedDto(toDto(challenge, currentAccount), joinCode);
    }

    /** Défis créés ou rejoints par l'appelant, triés par {@code closesAt} décroissant puis id. */
    public Page<ChallengeDto> listMyChallenges(CurrentAccountDto account, PageQuery pageQuery) {
        var fingerprint = CursorCodec.fingerprint(java.util.Map.of());
        OffsetDateTime afterClosesAt = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterClosesAt = OffsetDateTime.parse(keys.get(0));
            afterId = UUID.fromString(keys.get(1));
        }

        Specification<Challenge> spec = (root, query, cb) -> {
            var participantSubquery = query.subquery(UUID.class);
            var participantRoot = participantSubquery.from(ChallengeParticipant.class);
            participantSubquery
                    .select(participantRoot.get("challenge").get("id"))
                    .where(cb.equal(participantRoot.get("account").get("id"), account.id()));
            return cb.or(cb.equal(root.get("creator").get("id"), account.id()), root.get("id").in(participantSubquery));
        };
        spec = spec.and(KeysetPredicates.afterDescending("closesAt", afterClosesAt, afterId));

        var pageable = PageRequest.of(0, pageQuery.limit() + 1, Sort.by(Sort.Order.desc("closesAt"), Sort.Order.asc("id")));
        var rows = challengeRepository.findAll(spec, pageable).getContent();
        var hasMore = rows.size() > pageQuery.limit();
        var items = (hasMore ? rows.subList(0, pageQuery.limit()) : rows).stream()
                .map(challenge -> toDto(challenge, account))
                .toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getClosesAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    /**
     * Rejoint un défi via son code d'invitation. Appel répété par un membre existant : idempotent.
     *
     * @throws NotFoundException     si le code est invalide
     * @throws InvalidStateException si le défi est fermé, ou si une pratique antérieure de cette
     *                                version hors défi interdit d'y participer
     */
    @Transactional
    public ChallengeDto joinChallenge(CurrentAccountDto account, String joinCode) {
        var challenge = challengeRepository.findByJoinCodeHash(secureTokenFactory.hash(joinCode))
                .orElseThrow(() -> new NotFoundException("Code d'invitation invalide."));

        var existing = challengeParticipantRepository.findByChallenge_IdAndAccount_Id(challenge.getId(), account.id());
        if (existing.isPresent()) {
            return toDto(challenge, account);
        }
        if (challenge.getState() != ChallengeState.OPEN) {
            throw new InvalidStateException("Ce défi n'est plus ouvert.");
        }
        var versionId = challenge.getExerciseVersion().getId();
        if (attemptRepository.existsByLearner_IdAndExerciseVersion_IdAndStatus(
                account.id(), versionId, AttemptStatus.SUBMITTED)
                || attemptRepository.existsRevealedHintOutsideChallenge(account.id(), versionId, challenge.getId())) {
            throw new InvalidStateException(
                    "Une pratique antérieure de cette version hors de ce défi interdit d'y participer.");
        }
        challengeParticipantRepository.save(new ChallengeParticipant(
                challenge, accountRepository.getReferenceById(account.id()), OffsetDateTime.now(clock)));
        return toDto(challenge, account);
    }

    /** @throws NotFoundException si le défi n'existe pas ou si l'appelant n'y participe pas */
    public ChallengeDto getChallenge(CurrentAccountDto account, UUID challengeId) {
        var challenge = findChallengeAsParticipant(account, challengeId);
        return toDto(challenge, account);
    }

    /**
     * Ferme le défi, révoquant le code d'invitation. Idempotent.
     *
     * @throws NotFoundException  si le défi n'existe pas
     * @throws ForbiddenException si l'appelant n'est pas le créateur
     */
    @Transactional
    public ChallengeDto closeChallenge(CurrentAccountDto account, UUID challengeId) {
        var challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException("Défi introuvable."));
        if (!challenge.getCreator().getId().equals(account.id())) {
            throw new ForbiddenException("Seul le créateur peut fermer ce défi.");
        }
        challenge.close();
        return toDto(challenge, account);
    }

    /**
     * Réponses soumises par les participants, comparables une fois que l'appelant a lui-même soumis.
     *
     * @throws NotFoundException  si le défi n'existe pas ou si l'appelant n'y participe pas
     * @throws ForbiddenException si l'appelant n'a pas encore soumis dans ce défi
     */
    public Page<SharedResponseDto> listSharedResponses(CurrentAccountDto account, UUID challengeId, PageQuery pageQuery) {
        findChallengeAsParticipant(account, challengeId);
        requireSubmitted(account, challengeId);

        var attempts = attemptRepository.findByChallengeIdAndStatusOrderBySubmittedAtAscIdAsc(
                challengeId, AttemptStatus.SUBMITTED);
        var items = attempts.stream().map(this::toSharedResponseDto).toList();
        // Pagination simplifiée en mémoire : les défis rassemblent un nombre restreint de participants (v1).
        var limited = items.size() > pageQuery.limit() ? items.subList(0, pageQuery.limit()) : items;
        return Page.of(limited, null);
    }

    private SharedResponseDto toSharedResponseDto(Attempt attempt) {
        var participant = challengeParticipantRepository
                .findByChallenge_IdAndAccount_Id(attempt.getChallengeId(), attempt.getLearner().getId())
                .orElseThrow(() -> new IllegalStateException("Participant introuvable pour une tentative soumise."));
        return new SharedResponseDto(
                participant.getId(), attempt.getLearner().getDisplayName(), attempt.getAnswer(), attempt.getSubmittedAt());
    }

    /**
     * @throws NotFoundException  si le défi n'existe pas ou si l'appelant n'y participe pas
     * @throws ForbiddenException si l'appelant n'a pas encore soumis dans ce défi
     */
    public Page<DiscussionCommentDto> listChallengeComments(CurrentAccountDto account, UUID challengeId, PageQuery pageQuery) {
        findChallengeAsParticipant(account, challengeId);
        requireSubmitted(account, challengeId);

        var fingerprint = CursorCodec.fingerprint(java.util.Map.of("challengeId", challengeId));
        OffsetDateTime afterCreatedAt = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterCreatedAt = OffsetDateTime.parse(keys.get(0));
            afterId = UUID.fromString(keys.get(1));
        }

        Specification<DiscussionComment> spec =
                (root, query, cb) -> cb.equal(root.get("challenge").get("id"), challengeId);
        spec = spec.and(KeysetPredicates.afterAscending("createdAt", afterCreatedAt, afterId));

        var pageable = PageRequest.of(0, pageQuery.limit() + 1, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        var rows = discussionCommentRepository.findAll(spec, pageable).getContent();
        var hasMore = rows.size() > pageQuery.limit();
        var items = (hasMore ? rows.subList(0, pageQuery.limit()) : rows).stream().map(DiscussionCommentDto::from).toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getCreatedAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    /**
     * Ajoute un commentaire au débrief collectif, autorisé même après fermeture du défi.
     *
     * @throws NotFoundException  si le défi n'existe pas ou si l'appelant n'y participe pas
     * @throws ForbiddenException si l'appelant n'a pas encore soumis dans ce défi
     */
    @Transactional
    public DiscussionCommentDto createChallengeComment(CurrentAccountDto account, UUID challengeId, String body) {
        var challenge = findChallengeAsParticipant(account, challengeId);
        requireSubmitted(account, challengeId);
        var comment = discussionCommentRepository.save(
                new DiscussionComment(challenge, accountRepository.getReferenceById(account.id()), body));
        return DiscussionCommentDto.from(comment);
    }

    private Challenge findChallengeAsParticipant(CurrentAccountDto account, UUID challengeId) {
        var challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NotFoundException("Défi introuvable."));
        challengeParticipantRepository
                .findByChallenge_IdAndAccount_Id(challengeId, account.id())
                .orElseThrow(() -> new NotFoundException("Défi introuvable."));
        return challenge;
    }

    private void requireSubmitted(CurrentAccountDto account, UUID challengeId) {
        var submitted = attemptRepository.findByChallengeIdAndLearner_Id(challengeId, account.id())
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .isPresent();
        if (!submitted) {
            throw new ForbiddenException("Une soumission dans ce défi est requise pour accéder à cette ressource.");
        }
    }

    private ChallengeDto toDto(Challenge challenge, CurrentAccountDto account) {
        var participantCount = (int) challengeParticipantRepository.countByChallenge_Id(challenge.getId());
        var responsesUnlocked = attemptRepository
                .findByChallengeIdAndLearner_Id(challenge.getId(), account.id())
                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                .isPresent();
        return ChallengeDto.from(challenge, participantCount, responsesUnlocked);
    }
}
