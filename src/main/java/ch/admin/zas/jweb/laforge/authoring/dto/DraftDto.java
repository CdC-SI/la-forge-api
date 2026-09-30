package ch.admin.zas.jweb.laforge.authoring.dto;

import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection privée d'un brouillon (schéma {@code Draft}). */
public record DraftDto(
        UUID id,
        UUID exerciseId,
        UUID authorId,
        DraftState state,
        int revision,
        int baseVersion,
        ExerciseContentInput content,
        Integer publishedVersion,
        OffsetDateTime updatedAt) {
}
