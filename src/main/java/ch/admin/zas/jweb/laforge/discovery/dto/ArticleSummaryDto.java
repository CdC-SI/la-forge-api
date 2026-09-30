package ch.admin.zas.jweb.laforge.discovery.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.discovery.domain.Article;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Projection résumée d'une fiche de veille, utilisée dans les listes et le tableau de bord. */
public record ArticleSummaryDto(
        UUID id,
        String title,
        String summary,
        List<UUID> topicIds,
        List<TechnologyRequirement> technologies,
        OffsetDateTime publishedAt) {

    public static ArticleSummaryDto from(Article article) {
        var topicIds = article.getTopics().stream().map(topic -> topic.getId()).collect(Collectors.toList());
        return new ArticleSummaryDto(
                article.getId(), article.getTitle(), article.getSummary(), topicIds, article.getTechnologies(),
                article.getPublishedAt());
    }
}
