package ch.admin.zas.jweb.laforge.practice.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Réponse à choix unique, corrigée automatiquement par égalité avec {@code correctChoiceIds}. */
public record SingleChoiceAnswer(
        @NotBlank @Size(max = 50) String choiceId,
        @NotBlank @Size(max = 10000) String reasoning,
        @Min(1) @Max(5) int confidence) implements Answer {
}
