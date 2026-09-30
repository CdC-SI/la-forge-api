package ch.admin.zas.jweb.laforge.authoring.dto;

import ch.admin.zas.jweb.laforge.authoring.domain.EditorialReview;
import ch.admin.zas.jweb.laforge.authoring.domain.ReviewDecision;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection de lecture d'une relecture éditoriale (schéma {@code EditorialReview}). */
public record EditorialReviewDto(UUID reviewerId, ReviewDecision decision, String comment, OffsetDateTime reviewedAt) {

    public static EditorialReviewDto from(EditorialReview review) {
        return new EditorialReviewDto(
                review.getReviewer().getId(), review.getDecision(), review.getComment(), review.getReviewedAt());
    }
}
