package ch.admin.zas.jweb.laforge.practice.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Réponse à choix multiple, corrigée automatiquement par égalité stricte des ensembles de choix
 * avec {@code correctChoiceIds}.
 */
public record MultipleChoiceAnswer(
        @NotEmpty @Size(max = 10) List<String> choiceIds,
        @NotBlank @Size(max = 10000) String reasoning,
        @Min(1) @Max(5) int confidence) implements Answer {

    public MultipleChoiceAnswer {
        choiceIds = List.copyOf(choiceIds);
    }
}
