package ch.admin.zas.jweb.laforge.authoring.dto;

import ch.admin.zas.jweb.laforge.authoring.domain.ReviewDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /authoring/drafts/{draftId}/review}. */
public record ReviewInput(@NotNull ReviewDecision decision, @NotBlank @Size(max = 20000) String comment) {
}
