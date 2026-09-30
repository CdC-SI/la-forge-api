package ch.admin.zas.jweb.laforge.authoring.service;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.authoring.domain.EditorialReview;
import ch.admin.zas.jweb.laforge.authoring.domain.ReviewDecision;
import ch.admin.zas.jweb.laforge.authoring.dto.DraftDto;
import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.authoring.repository.DraftRepository;
import ch.admin.zas.jweb.laforge.authoring.repository.EditorialReviewRepository;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.KeysetPredicates;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cycle de vie éditorial des brouillons d'exercice : création, remplacement, relecture et
 * publication immuable (machine à états {@code DRAFT → IN_REVIEW → APPROVED → PUBLISHED}).
 */
@Service
@Transactional(readOnly = true)
public class AuthoringService {

    private final DraftRepository draftRepository;
    private final EditorialReviewRepository editorialReviewRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseVersionRepository exerciseVersionRepository;
    private final TopicRepository topicRepository;
    private final AccountRepository accountRepository;
    private final Clock clock;

    public AuthoringService(
            DraftRepository draftRepository,
            EditorialReviewRepository editorialReviewRepository,
            ExerciseRepository exerciseRepository,
            ExerciseVersionRepository exerciseVersionRepository,
            TopicRepository topicRepository,
            AccountRepository accountRepository,
            Clock clock) {
        this.draftRepository = draftRepository;
        this.editorialReviewRepository = editorialReviewRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseVersionRepository = exerciseVersionRepository;
        this.topicRepository = topicRepository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    /**
     * Brouillons accessibles à l'appelant selon ses rôles cumulatifs : AUTHOR voit les siens,
     * REVIEWER voit les {@code IN_REVIEW} et ceux qu'il a déjà relus, ADMIN voit tout.
     */
    public Page<DraftDto> listDrafts(CurrentAccountDto account, PageQuery pageQuery, DraftState state) {
        var filters = new HashMap<String, Object>();
        filters.put("state", state);
        var fingerprint = CursorCodec.fingerprint(filters);

        OffsetDateTime afterUpdatedAt = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterUpdatedAt = OffsetDateTime.parse(keys.get(0));
            afterId = UUID.fromString(keys.get(1));
        }

        Specification<Draft> spec = visibilitySpecification(account);
        if (state != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("state"), state));
        }
        spec = spec.and(KeysetPredicates.afterDescending("updatedAt", afterUpdatedAt, afterId));

        var pageable = PageRequest.of(0, pageQuery.limit() + 1, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id")));
        var rows = draftRepository.findAll(spec, pageable).getContent();
        var hasMore = rows.size() > pageQuery.limit();
        var items = (hasMore ? rows.subList(0, pageQuery.limit()) : rows).stream()
                .map(draft -> DraftDto.from(draft, editorialReviewRepository))
                .toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getUpdatedAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    private Specification<Draft> visibilitySpecification(CurrentAccountDto account) {
        if (account.hasAnyRole(Set.of(Role.ADMIN))) {
            return (root, query, cb) -> cb.conjunction();
        }
        var reviewedDraftIds = account.hasAnyRole(Set.of(Role.REVIEWER))
                ? editorialReviewRepository.findDistinctDraftIdsByReviewer(account.id())
                : List.<UUID>of();
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (account.hasAnyRole(Set.of(Role.AUTHOR))) {
                predicates.add(cb.equal(root.get("author").get("id"), account.id()));
            }
            if (account.hasAnyRole(Set.of(Role.REVIEWER))) {
                predicates.add(cb.equal(root.get("state"), DraftState.IN_REVIEW));
                if (!reviewedDraftIds.isEmpty()) {
                    predicates.add(root.get("id").in(reviewedDraftIds));
                }
            }
            return predicates.isEmpty() ? cb.disjunction() : cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /** @throws NotFoundException si le brouillon n'existe pas ou est hors du périmètre de l'appelant */
    public DraftDto getDraft(CurrentAccountDto account, UUID draftId) {
        var draft = findVisibleDraft(account, draftId);
        return DraftDto.from(draft, editorialReviewRepository);
    }

    /**
     * Crée un brouillon (nouvel exercice) ou une nouvelle révision (exercice existant dont
     * l'appelant est auteur ou {@code ADMIN}).
     *
     * @throws NotFoundException     si {@code exerciseId} est fourni mais introuvable
     * @throws ForbiddenException    si l'appelant n'est ni auteur de l'exercice ni {@code ADMIN}
     * @throws InvalidStateException si une révision non publiée existe déjà pour cet exercice
     */
    @Transactional
    public DraftDto createDraft(CurrentAccountDto account, UUID exerciseId, ExerciseContentInput content) {
        var isAdmin = account.hasAnyRole(Set.of(Role.ADMIN));
        Exercise exercise;
        int baseVersion;
        if (exerciseId == null) {
            exercise = exerciseRepository.save(new Exercise());
            baseVersion = 0;
        } else {
            exercise = exerciseRepository.findById(exerciseId)
                    .orElseThrow(() -> new NotFoundException("Exercice introuvable."));
            if (!isAdmin && !draftRepository.existsByExercise_IdAndAuthor_Id(exerciseId, account.id())) {
                throw new ForbiddenException("Seul l'auteur de l'exercice ou un administrateur peut ouvrir une révision.");
            }
            draftRepository.findByExerciseIdAndStateNot(exerciseId, DraftState.PUBLISHED).ifPresent(existing -> {
                throw new InvalidStateException("Une révision non publiée existe déjà pour cet exercice.");
            });
            baseVersion = exerciseVersionRepository
                    .findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(exerciseId)
                    .map(ExerciseVersion::getVersionNumber)
                    .orElse(0);
        }

        var topics = resolveTopics(content.topicIds());
        var author = accountRepository.getReferenceById(account.id());
        var draft = new Draft(
                exercise,
                author,
                baseVersion,
                content.title(),
                content.type(),
                content.difficulty(),
                content.estimatedMinutes(),
                content.promptMarkdown(),
                content.learningObjectives(),
                content.files(),
                content.technologies(),
                content.responseSpec(),
                content.hints(),
                content.correction(),
                topics);
        return DraftDto.from(draftRepository.save(draft), editorialReviewRepository);
    }

    /**
     * Remplace intégralement le contenu d'un brouillon {@code DRAFT}.
     *
     * @throws NotFoundException                                             si le brouillon n'existe pas ou est hors périmètre
     * @throws ForbiddenException                                            si l'appelant n'est ni auteur ni {@code ADMIN}
     * @throws InvalidStateException                                        si le brouillon n'est pas {@code DRAFT}
     * @throws ch.admin.zas.jweb.laforge.common.error.StaleVersionException si {@code expectedRevision} est obsolète
     */
    @Transactional
    public DraftDto replaceDraft(CurrentAccountDto account, UUID draftId, int expectedRevision, ExerciseContentInput content) {
        var draft = findOwnedDraft(account, draftId);
        var topics = resolveTopics(content.topicIds());
        draft.replaceContent(
                expectedRevision,
                content.title(),
                content.type(),
                content.difficulty(),
                content.estimatedMinutes(),
                content.promptMarkdown(),
                content.learningObjectives(),
                content.files(),
                content.technologies(),
                content.responseSpec(),
                content.hints(),
                content.correction(),
                topics);
        return DraftDto.from(draft, editorialReviewRepository);
    }

    /**
     * Soumet le brouillon à relecture après vérification de cohérence complète du contenu.
     *
     * @throws NotFoundException         si le brouillon n'existe pas ou est hors périmètre
     * @throws ForbiddenException        si l'appelant n'est ni auteur ni {@code ADMIN}
     * @throws ValidationFailedException si le contenu est incohérent (indices, choix, thèmes)
     */
    @Transactional
    public DraftDto submitDraftForReview(CurrentAccountDto account, UUID draftId, int expectedRevision) {
        var draft = findOwnedDraft(account, draftId);
        validateConsistency(draft);
        draft.submitForReview(expectedRevision);
        return DraftDto.from(draft, editorialReviewRepository);
    }

    private void validateConsistency(Draft draft) {
        var hints = draft.getHints();
        for (int i = 0; i < hints.size(); i++) {
            if (hints.get(i).level() != i + 1) {
                throw new ValidationFailedException("Les indices doivent être contigus à partir du niveau 1.");
            }
        }
        if (draft.getTopics().isEmpty()) {
            throw new ValidationFailedException("Au moins un thème doit être associé au brouillon.");
        }
        var spec = draft.getResponseSpec();
        var correction = draft.getCorrection();
        switch (spec.kind()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> {
                var choiceIds = spec.choices().stream().map(choice -> choice.id()).collect(java.util.stream.Collectors.toSet());
                if (correction.correctChoiceIds().isEmpty()
                        || !choiceIds.containsAll(correction.correctChoiceIds())) {
                    throw new ValidationFailedException("Le corrigé doit référencer des choix existants du format de réponse.");
                }
            }
            case REVIEW -> {
                if (correction.expectedVerdicts().isEmpty()) {
                    throw new ValidationFailedException("Un exercice de revue doit définir au moins un verdict attendu.");
                }
            }
            case FREE_TEXT -> {
                // Aucune contrainte supplémentaire pour une réponse libre.
            }
        }
    }

    /**
     * Approuve ou renvoie en brouillon une révision {@code IN_REVIEW}. Toujours consigné dans
     * l'historique des relectures, quelle que soit la décision.
     *
     * @throws NotFoundException                                            si le brouillon n'existe pas ou est hors périmètre
     * @throws InvalidStateException                                        si le brouillon n'est pas {@code IN_REVIEW}
     * @throws ForbiddenException                                           si le relecteur est l'auteur (hors {@code ADMIN})
     * @throws ch.admin.zas.jweb.laforge.common.error.StaleVersionException si {@code expectedRevision} est obsolète
     */
    @Transactional
    public DraftDto reviewDraft(CurrentAccountDto reviewer, UUID draftId, int expectedRevision, ReviewDecision decision, String comment) {
        var draft = findVisibleDraft(reviewer, draftId);
        var now = OffsetDateTime.now(clock);
        var reviewerIsAdmin = reviewer.hasAnyRole(Set.of(Role.ADMIN));
        var reviewerEntity = accountRepository.getReferenceById(reviewer.id());
        if (decision == ReviewDecision.APPROVE) {
            draft.approve(expectedRevision, reviewerEntity, reviewerIsAdmin);
        } else {
            draft.requestChanges(expectedRevision);
        }
        editorialReviewRepository.save(new EditorialReview(draft, reviewerEntity, decision, comment, now));
        return DraftDto.from(draft, editorialReviewRepository);
    }

    /**
     * Publie la révision approuvée en une nouvelle version immuable de l'exercice.
     *
     * @throws NotFoundException                                            si le brouillon n'existe pas ou est hors périmètre
     * @throws ForbiddenException                                            si l'appelant n'est ni auteur ni {@code ADMIN}
     * @throws InvalidStateException                                        si le brouillon n'est pas {@code APPROVED}, ou si
     *                                                                       {@code baseVersion} n'est plus la dernière publiée
     * @throws ch.admin.zas.jweb.laforge.common.error.StaleVersionException si {@code expectedRevision} est obsolète
     */
    @Transactional
    public ExerciseDto publishDraft(CurrentAccountDto account, UUID draftId, int expectedRevision) {
        var draft = findOwnedDraft(account, draftId);
        var latestPublished = exerciseVersionRepository
                .findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(draft.getExercise().getId())
                .map(ExerciseVersion::getVersionNumber)
                .orElse(0);
        if (latestPublished != draft.getBaseVersion()) {
            throw new InvalidStateException("Une version plus récente a déjà été publiée pour cet exercice.");
        }
        var newVersionNumber = draft.getBaseVersion() + 1;
        var now = OffsetDateTime.now(clock);
        var version = new ExerciseVersion(
                draft.getExercise(),
                newVersionNumber,
                draft.getTitle(),
                draft.getType(),
                draft.getDifficulty(),
                draft.getEstimatedMinutes(),
                draft.getPromptMarkdown(),
                draft.getLearningObjectives(),
                draft.getFiles(),
                draft.getTechnologies(),
                draft.getResponseSpec(),
                draft.getHints(),
                draft.getCorrection(),
                draft.getTopics());
        version.publish(now);
        version = exerciseVersionRepository.save(version);

        draft.publish(expectedRevision, newVersionNumber);
        return ExerciseDto.from(version);
    }

    private Draft findOwnedDraft(CurrentAccountDto account, UUID draftId) {
        var draft = draftRepository.findById(draftId).orElseThrow(() -> new NotFoundException("Brouillon introuvable."));
        if (!account.hasAnyRole(Set.of(Role.ADMIN)) && !draft.getAuthor().getId().equals(account.id())) {
            throw new ForbiddenException("Seul l'auteur du brouillon ou un administrateur peut effectuer cette action.");
        }
        return draft;
    }

    private Draft findVisibleDraft(CurrentAccountDto account, UUID draftId) {
        var draft = draftRepository.findById(draftId).orElseThrow(() -> new NotFoundException("Brouillon introuvable."));
        var isAdmin = account.hasAnyRole(Set.of(Role.ADMIN));
        var isOwner = account.hasAnyRole(Set.of(Role.AUTHOR)) && draft.getAuthor().getId().equals(account.id());
        var isReviewerVisible = account.hasAnyRole(Set.of(Role.REVIEWER))
                && (draft.getState() == DraftState.IN_REVIEW
                        || !editorialReviewRepository.findByDraftOrderByReviewedAtAsc(draft).stream()
                                .noneMatch(review -> review.getReviewer().getId().equals(account.id())));
        if (!isAdmin && !isOwner && !isReviewerVisible) {
            throw new NotFoundException("Brouillon introuvable.");
        }
        return draft;
    }

    private LinkedHashSet<Topic> resolveTopics(List<UUID> topicIds) {
        var topics = new LinkedHashSet<Topic>();
        for (var topicId : topicIds) {
            topics.add(topicRepository.findById(topicId)
                    .orElseThrow(() -> new NotFoundException("Thème introuvable : " + topicId)));
        }
        return topics;
    }
}
