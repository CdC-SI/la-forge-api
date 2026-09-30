package ch.admin.zas.jweb.laforge.authoring.dto;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.authoring.repository.EditorialReviewRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Projection de lecture d'un brouillon et de son historique de relecture (schéma {@code Draft}). */
public record DraftDto(
        UUID id,
        UUID exerciseId,
        UUID authorId,
        DraftState state,
        int revision,
        int baseVersion,
        ExerciseContentInput content,
        List<EditorialReviewDto> reviews,
        Integer publishedVersion,
        OffsetDateTime updatedAt) {

    public static DraftDto from(Draft draft, EditorialReviewRepository editorialReviewRepository) {
        var reviews = editorialReviewRepository.findByDraftOrderByReviewedAtAsc(draft).stream()
                .map(EditorialReviewDto::from)
                .toList();
        return new DraftDto(
                draft.getId(),
                draft.getExercise().getId(),
                draft.getAuthor().getId(),
                draft.getState(),
                draft.getRevision(),
                draft.getBaseVersion(),
                ExerciseContentInput.from(draft),
                reviews,
                draft.getPublishedVersion(),
                draft.getUpdatedAt());
    }
}
