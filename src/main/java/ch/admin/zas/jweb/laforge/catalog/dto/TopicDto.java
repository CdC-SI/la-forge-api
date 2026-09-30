package ch.admin.zas.jweb.laforge.catalog.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.Topic;

/** Projection de lecture d'un thème du référentiel. */
public record TopicDto(java.util.UUID id, String slug, String label) {

    public static TopicDto from(Topic topic) {
        return new TopicDto(topic.getId(), topic.getSlug(), topic.getLabel());
    }
}
