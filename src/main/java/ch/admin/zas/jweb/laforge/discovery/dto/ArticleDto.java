package ch.admin.zas.jweb.laforge.discovery.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.discovery.domain.Article;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Projection complète d'une fiche de veille. */
public record ArticleDto(
        UUID id,
        String title,
        String summary,
        List<UUID> topicIds,
        List<TechnologyRequirement> technologies,
        OffsetDateTime publishedAt,
        OffsetDateTime updatedAt,
        String bodyMarkdown,
        List<Source> sources,
        List<ExerciseSummaryDto> relatedExercises,
        int revision) {

    public static ArticleDto from(Article article) {
        var topicIds = article.getTopics().stream().map(topic -> topic.getId()).collect(Collectors.toList());
        var related = article.getRelatedExerciseVersions().stream().map(ExerciseSummaryDto::from).collect(Collectors.toList());
        return new ArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                topicIds,
                article.getTechnologies(),
                article.getPublishedAt(),
                article.getUpdatedAt(),
                article.getBodyMarkdown(),
                article.getSources(),
                related,
                article.getRevision());
    }
}
