package ch.admin.zas.jweb.laforge.collective.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /challenges/{challengeId}/comments}. Texte brut, jamais interprété. */
public record CommentInput(@NotBlank @Size(max = 4000) String body) {
}
