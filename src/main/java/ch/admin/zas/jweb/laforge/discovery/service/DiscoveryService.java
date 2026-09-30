package ch.admin.zas.jweb.laforge.discovery.service;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.KeysetPredicates;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.discovery.domain.Article;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleDto;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleInput;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleSummaryDto;
import ch.admin.zas.jweb.laforge.discovery.repository.ArticleRepository;
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

/** Lecture et publication des fiches de veille technique (tags {@code Discoveries}/{@code Authoring}). */
@Service
@Transactional(readOnly = true)
public class DiscoveryService {

    private final ArticleRepository articleRepository;
    private final TopicRepository topicRepository;
    private final ExerciseVersionRepository exerciseVersionRepository;
    private final Clock clock;

    public DiscoveryService(
            ArticleRepository articleRepository,
            TopicRepository topicRepository,
            ExerciseVersionRepository exerciseVersionRepository,
            Clock clock) {
        this.articleRepository = articleRepository;
        this.topicRepository = topicRepository;
        this.exerciseVersionRepository = exerciseVersionRepository;
        this.clock = clock;
    }

    /** Fiches publiées, filtrées par thème/technologie/texte libre (ET), triées par date décroissante. */
    public Page<ArticleSummaryDto> listArticles(PageQuery pageQuery, UUID topicId, String technology, String q) {
        var filters = new HashMap<String, Object>();
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

        Specification<Article> spec = (root, query, cb) -> cb.conjunction();
        if (topicId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("topics").get("id"), topicId);
            });
        }
        if (q != null && !q.isBlank()) {
            var like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("summary")), like)));
        }
        spec = spec.and(KeysetPredicates.afterDescending("publishedAt", afterPublishedAt, afterId));

        var pageable = PageRequest.of(
                0, pageQuery.limit() + 1, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.asc("id")));
        var rows = articleRepository.findAll(spec, pageable).getContent();

        var filtered = technology == null || technology.isBlank()
                ? rows
                : rows.stream()
                        .filter(article -> article.getTechnologies().stream()
                                .anyMatch(tech -> tech.technology().equalsIgnoreCase(technology)))
                        .toList();

        var hasMore = rows.size() > pageQuery.limit();
        var pageRows = hasMore ? filtered.subList(0, Math.min(filtered.size(), pageQuery.limit())) : filtered;
        var items = pageRows.stream().map(ArticleSummaryDto::from).toList();
        String nextCursor = null;
        if (hasMore) {
            var last = rows.get(pageQuery.limit() - 1);
            nextCursor = CursorCodec.encode(fingerprint, List.of(last.getPublishedAt().toString(), last.getId().toString()));
        }
        return Page.of(items, nextCursor);
    }

    /** @throws NotFoundException si la fiche n'existe pas */
    public ArticleDto getArticle(UUID articleId) {
        return ArticleDto.from(findArticleOrThrow(articleId));
    }

    /**
     * Publication immédiate, réservée à {@code REVIEWER}/{@code ADMIN}.
     *
     * @throws NotFoundException si un thème ou un exercice référencé n'existe pas ou n'est pas publié
     */
    @Transactional
    public ArticleDto publishArticle(ArticleInput input) {
        var topics = resolveTopics(input.topicIds());
        var relatedVersions = resolveRelatedVersions(input.relatedExercises());
        var article = new Article(
                input.title(),
                input.summary(),
                input.bodyMarkdown(),
                input.technologies(),
                input.sources(),
                topics,
                relatedVersions,
                OffsetDateTime.now(clock));
        return ArticleDto.from(articleRepository.save(article));
    }

    /**
     * @throws NotFoundException                                        si la fiche, un thème ou un exercice référencé n'existe pas
     * @throws ch.admin.zas.jweb.laforge.common.error.StaleVersionException si {@code expectedRevision} est obsolète
     */
    @Transactional
    public ArticleDto updateArticle(UUID articleId, int expectedRevision, ArticleInput content) {
        var article = findArticleOrThrow(articleId);
        var topics = resolveTopics(content.topicIds());
        var relatedVersions = resolveRelatedVersions(content.relatedExercises());
        article.update(
                expectedRevision,
                content.title(),
                content.summary(),
                content.bodyMarkdown(),
                content.technologies(),
                content.sources(),
                topics,
                relatedVersions);
        return ArticleDto.from(article);
    }

    private Article findArticleOrThrow(UUID articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Fiche de veille introuvable."));
    }

    private LinkedHashSet<Topic> resolveTopics(List<UUID> topicIds) {
        var topics = new LinkedHashSet<Topic>();
        for (var topicId : topicIds) {
            topics.add(topicRepository.findById(topicId)
                    .orElseThrow(() -> new NotFoundException("Thème introuvable : " + topicId)));
        }
        return topics;
    }

    private LinkedHashSet<ExerciseVersion> resolveRelatedVersions(
            List<ch.admin.zas.jweb.laforge.discovery.dto.ExerciseReferenceDto> references) {
        var versions = new LinkedHashSet<ExerciseVersion>();
        for (var reference : references) {
            var version = exerciseVersionRepository
                    .findByExercise_IdAndVersionNumber(reference.exerciseId(), reference.version())
                    .filter(ExerciseVersion::isPublished)
                    .orElseThrow(() -> new NotFoundException(
                            "Exercice ou version introuvable ou non publiée : " + reference.exerciseId()));
            versions.add(version);
        }
        return versions;
    }
}
