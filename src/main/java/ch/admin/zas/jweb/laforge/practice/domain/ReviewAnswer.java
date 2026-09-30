package ch.admin.zas.jweb.laforge.practice.domain;

import ch.admin.zas.jweb.laforge.catalog.domain.ReviewVerdict;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Réponse de type revue de code : verdict global et commentaires ancrés ligne à ligne. */
public record ReviewAnswer(
        @NotNull ReviewVerdict verdict,
        @Size(max = 30) List<LineComment> comments,
        @NotBlank @Size(max = 10000) String reasoning,
        @Min(1) @Max(5) int confidence) implements Answer {

    public ReviewAnswer {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }
}
