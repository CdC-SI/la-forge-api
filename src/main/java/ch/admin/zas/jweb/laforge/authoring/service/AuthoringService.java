package ch.admin.zas.jweb.laforge.authoring.service;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.authoring.dto.DraftDto;
import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.authoring.mapper.DraftMapper;
import ch.admin.zas.jweb.laforge.authoring.repository.DraftRepository;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.KeysetPredicates;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Brouillons privés et publication directe d'une version immuable par son auteur. */
@Service
@Transactional(readOnly = true)
public class AuthoringService {

    private final DraftRepository draftRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseVersionRepository exerciseVersionRepository;
    private final TopicRepository topicRepository;
    private final AccountRepository accountRepository;
    private final PublicationValidator publicationValidator;
    private final ExerciseCompletionService completionService;
    private final Clock clock;

    public AuthoringService(
            DraftRepository draftRepository,
            ExerciseRepository exerciseRepository,
            ExerciseVersionRepository exerciseVersionRepository,
            TopicRepository topicRepository,
            AccountRepository accountRepository,
            PublicationValidator publicationValidator,
            ExerciseCompletionService completionService,
            Clock clock) {
        this.draftRepository = draftRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseVersionRepository = exerciseVersionRepository;
        this.topicRepository = topicRepository;
        this.accountRepository = accountRepository;
        this.publicationValidator = publicationValidator;
        this.completionService = completionService;
        this.clock = clock;
    }

    public Page<DraftDto> listDrafts(CurrentAccountDto account, PageQuery pageQuery, DraftState state) {
        var filters = new HashMap<String, Object>();
        filters.put("state", state);
        filters.put("authorId", account.id());
        var fingerprint = CursorCodec.fingerprint(filters);
        OffsetDateTime afterUpdatedAt = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterUpdatedAt = OffsetDateTime.parse(keys.get(0));
            afterId = UUID.fromString(keys.get(1));
        }
        Specification<Draft> spec = (root, query, cb) -> cb.equal(root.get("author").get("id"), account.id());
        if (state != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("state"), state));
        }
        spec = spec.and(KeysetPredicates.afterDescending("updatedAt", afterUpdatedAt, afterId));
        var pageable = PageRequest.of(0, pageQuery.limit() + 1,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id")));
        var rows = draftRepository.findAll(spec, pageable).getContent();
        var hasMore = rows.size() > pageQuery.limit();
        var items = (hasMore ? rows.subList(0, pageQuery.limit()) : rows).stream()
                .map(DraftMapper::toDto).toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getUpdatedAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    public DraftDto getDraft(CurrentAccountDto account, UUID draftId) {
        return DraftMapper.toDto(findOwnedDraft(account, draftId));
    }

    @Transactional
    public DraftDto createDraft(CurrentAccountDto account, UUID exerciseId, ExerciseContentInput content) {
        Exercise exercise;
        int baseVersion;
        if (exerciseId == null) {
            exercise = exerciseRepository.save(new Exercise());
            baseVersion = 0;
        } else {
            if (!draftRepository.existsByExercise_IdAndAuthor_Id(exerciseId, account.id())) {
                throw new NotFoundException("Exercice introuvable.");
            }
            // Le verrou sérialise l'ouverture d'une révision sur un exercice existant.
            exercise = exerciseRepository.findByIdForUpdate(exerciseId)
                    .orElseThrow(() -> new NotFoundException("Exercice introuvable."));
            if (draftRepository.findByExerciseIdAndStateNot(exerciseId, DraftState.PUBLISHED).isPresent()) {
                throw new InvalidStateException("Une révision non publiée existe déjà pour cet exercice.");
            }
            baseVersion = latestPublishedVersion(exerciseId);
        }
        var topics = resolveTopics(content.topicIds());
        var author = accountRepository.getReferenceById(account.id());
        var draft = new Draft(
                exercise, author, baseVersion, content.title(), content.type(), content.difficulty(),
                content.estimatedMinutes(), content.promptMarkdown(), content.learningObjectives(),
                content.files(), content.technologies(), content.responseSpec(), content.hints(),
                content.correction(), topics);
        return DraftMapper.toDto(draftRepository.saveAndFlush(draft));
    }

    @Transactional
    public DraftDto replaceDraft(CurrentAccountDto account, UUID draftId, int expectedRevision, ExerciseContentInput content) {
        var draft = findOwnedDraft(account, draftId);
        draft.requireEditable(expectedRevision);
        var topics = resolveTopics(content.topicIds());
        draft.replaceContent(
                expectedRevision, content.title(), content.type(), content.difficulty(),
                content.estimatedMinutes(), content.promptMarkdown(), content.learningObjectives(),
                content.files(), content.technologies(), content.responseSpec(), content.hints(),
                content.correction(), topics);
        draftRepository.flush();
        return DraftMapper.toDto(draft);
    }

    @Transactional
    public ExerciseDto publishDraft(CurrentAccountDto account, UUID draftId, int expectedRevision) {
        var draft = findOwnedDraft(account, draftId);
        draft.requireEditable(expectedRevision);
        publicationValidator.validate(DraftMapper.toContent(draft));
        if (latestPublishedVersion(draft.getExercise().getId()) != draft.getBaseVersion()) {
            throw new InvalidStateException("Une version plus récente a déjà été publiée pour cet exercice.");
        }
        var newVersionNumber = draft.getBaseVersion() + 1;
        var version = new ExerciseVersion(
                draft.getExercise(), newVersionNumber, draft.getTitle(), draft.getType(), draft.getDifficulty(),
                draft.getEstimatedMinutes(), draft.getPromptMarkdown(), draft.getLearningObjectives(),
                draft.getFiles(), draft.getTechnologies(), draft.getResponseSpec(), draft.getHints(),
                draft.getCorrection(), draft.getTopics());
        version.publish(OffsetDateTime.now(clock));
        draft.publish(expectedRevision, newVersionNumber);
        // Détecter une publication concurrente avant d'insérer une nouvelle version.
        draftRepository.flush();
        version = exerciseVersionRepository.save(version);
        var exerciseId = version.getExercise().getId();
        return ExerciseDto.from(version, completionService.completedExerciseIds(account.id(), List.of(exerciseId))
                .contains(exerciseId));
    }

    private int latestPublishedVersion(UUID exerciseId) {
        return exerciseVersionRepository.findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(exerciseId)
                .map(ExerciseVersion::getVersionNumber).orElse(0);
    }

    private Draft findOwnedDraft(CurrentAccountDto account, UUID draftId) {
        return draftRepository.findByIdAndAuthor_Id(draftId, account.id())
                .orElseThrow(() -> new NotFoundException("Brouillon introuvable."));
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
