package ch.admin.zas.jweb.laforge.review.dto;

import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.review.domain.ReviewItem;
import ch.admin.zas.jweb.laforge.review.domain.ReviewReason;
import ch.admin.zas.jweb.laforge.review.domain.ReviewState;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection de lecture d'une fiche de révision (schéma {@code ReviewItem}). */
public record ReviewItemDto(
        UUID id, ExerciseSummaryDto exercise, UUID sourceAttemptId, OffsetDateTime dueAt, ReviewReason reason, ReviewState state) {

    public static ReviewItemDto from(ReviewItem item, Clock clock) {
        return new ReviewItemDto(
                item.getId(),
                ExerciseSummaryDto.from(item.getExerciseVersion()),
                item.getSourceAttempt().getId(),
                item.getDueAt(),
                item.getReason(),
                item.state(OffsetDateTime.now(clock)));
    }
}
