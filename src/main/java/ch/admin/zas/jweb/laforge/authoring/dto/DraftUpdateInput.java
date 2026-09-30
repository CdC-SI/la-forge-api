package ch.admin.zas.jweb.laforge.authoring.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Corps de {@code PUT /authoring/drafts/{draftId}}. */
public record DraftUpdateInput(@NotNull @Valid ExerciseContentInput content) {
}
