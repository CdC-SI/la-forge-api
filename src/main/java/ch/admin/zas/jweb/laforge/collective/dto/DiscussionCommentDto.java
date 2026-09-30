package ch.admin.zas.jweb.laforge.collective.dto;

import ch.admin.zas.jweb.laforge.collective.domain.DiscussionComment;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection de lecture d'un commentaire du débrief collectif (schéma {@code DiscussionComment}). */
public record DiscussionCommentDto(UUID id, String authorDisplayName, String body, OffsetDateTime createdAt) {

    public static DiscussionCommentDto from(DiscussionComment comment) {
        return new DiscussionCommentDto(
                comment.getId(), comment.getAuthor().getDisplayName(), comment.getBody(), comment.getCreatedAt());
    }
}
