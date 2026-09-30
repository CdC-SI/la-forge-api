package ch.admin.zas.jweb.laforge.catalog.service;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicDto;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.KeysetPredicates;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lecture du catalogue public : thèmes et dernière version publiée de chaque exercice. Projection
 * apprenant uniquement — ni corrigé, ni indices non débloqués (voir {@link ExerciseDto}).
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final TopicRepository topicRepository;
    private final ExerciseVersionRepository exerciseVersionRepository;

    public CatalogService(TopicRepository topicRepository, ExerciseVersionRepository exerciseVersionRepository) {
        this.topicRepository = topicRepository;
        this.exerciseVersionRepository = exerciseVersionRepository;
    }

    /** Thèmes du référentiel, triés par libellé puis id. */
    public Page<TopicDto> listTopics(PageQuery pageQuery) {
        var fingerprint = CursorCodec.fingerprint(Map.of());
        String afterLabel = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterLabel = keys.get(0);
            afterId = UUID.fromString(keys.get(1));
        }
        Specification<Topic> spec = KeysetPredicates.afterAscending("label", afterLabel, afterId);
        var pageable = PageRequest.of(0, pageQuery.limit() + 1, Sort.by(Sort.Order.asc("label"), Sort.Order.asc("id")));
        var rows = topicRepository.findAll(spec, pageable).getContent();
        var hasMore = rows.size() > pageQuery.limit();
        var items = (hasMore ? rows.subList(0, pageQuery.limit()) : rows).stream().map(TopicDto::from).toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getLabel(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    /**
     * Dernière version publiée de chaque exercice, filtrée par type/difficulté/thème/technologie/
     * texte libre (ET logique), triée par {@code publishedAt} décroissant puis {@code id}. Le
     * filtre {@code technology} est appliqué après lecture de la page (contenu {@code jsonb}, non
     * indexable simplement en JPA Criteria) : une page peut alors contenir moins d'éléments que la
     * limite demandée lorsqu'il est utilisé, simplification documentée pour la v1.
     */
    public Page<ExerciseSummaryDto> listExercises(
            PageQuery pageQuery, ExerciseType type, Difficulty difficulty, UUID topicId, String technology, String q) {
        var filters = new HashMap<String, Object>();
        filters.put("type", type);
        filters.put("difficulty", difficulty);
        filters.put("topicId", topicId);
        filters.put("technology", technology);
        filters.put("q", q);
        var fingerprint = CursorCodec.fingerprint(filters);

        OffsetDateTime afterPublishedAt = null;
        UUID afterId = null;
        if (pageQuery.hasCursor()) {
            var keys = CursorCodec.decode(pageQuery.cursor(), fingerprint);
            afterPublishedAt = OffsetDateTime.parse(keys.get(0));
            afterId = UUID.fromString(keys.get(1));
        }

        Specification<ExerciseVersion> spec = latestPublishedVersion();
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (difficulty != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("difficulty"), difficulty));
        }
        if (topicId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("topics").get("id"), topicId);
            });
        }
        if (q != null && !q.isBlank()) {
            var like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("promptMarkdown")), like)));
        }
        spec = spec.and(KeysetPredicates.afterDescending("publishedAt", afterPublishedAt, afterId));

        var pageable = PageRequest.of(
                0, pageQuery.limit() + 1, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.asc("id")));
        var rows = exerciseVersionRepository.findAll(spec, pageable).getContent();

        var filtered = technology == null || technology.isBlank()
                ? rows
                : rows.stream()
                        .filter(version -> version.getTechnologies().stream()
                                .anyMatch(tech -> tech.technology().equalsIgnoreCase(technology)))
                        .toList();

        var hasMore = rows.size() > pageQuery.limit();
        var pageRows = hasMore ? filtered.subList(0, Math.min(filtered.size(), pageQuery.limit())) : filtered;
        var items = pageRows.stream().map(ExerciseSummaryDto::from).toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getPublishedAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    /** @throws NotFoundException si l'exercice n'existe pas ou n'a aucune version publiée */
    public ExerciseDto getLatestExercise(UUID exerciseId) {
        var version = exerciseVersionRepository
                .findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(exerciseId)
                .orElseThrow(() -> new NotFoundException("Aucune version publiée pour cet exercice."));
        return ExerciseDto.from(version);
    }

    /** @throws NotFoundException si cette version n'existe pas ou n'est pas publiée */
    public ExerciseDto getExerciseVersion(UUID exerciseId, int versionNumber) {
        var version = exerciseVersionRepository
                .findByExercise_IdAndVersionNumber(exerciseId, versionNumber)
                .filter(ExerciseVersion::isPublished)
                .orElseThrow(() -> new NotFoundException("Cette version d'exercice n'existe pas ou n'est pas publiée."));
        return ExerciseDto.from(version);
    }

    private static Specification<ExerciseVersion> latestPublishedVersion() {
        return (root, query, cb) -> {
            var subquery = query.subquery(Integer.class);
            var subRoot = subquery.from(ExerciseVersion.class);
            subquery.select(cb.max(subRoot.get("versionNumber")))
                    .where(cb.equal(subRoot.get("exercise"), root.get("exercise")), cb.isNotNull(subRoot.get("publishedAt")));
            return cb.and(cb.isNotNull(root.get("publishedAt")), cb.equal(root.get("versionNumber"), subquery));
        };
    }
}
