package ch.admin.zas.jweb.laforge.practice.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Réponse rédigée librement, jamais notée automatiquement ({@link ObjectiveResult#NOT_AUTO_GRADED}). */
public record FreeTextAnswer(
        @NotBlank @Size(max = 20000) String text,
        @NotBlank @Size(max = 10000) String reasoning,
        @Min(1) @Max(5) int confidence) implements Answer {
}
