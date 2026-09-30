package ch.admin.zas.jweb.laforge.catalog.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Format de réponse attendu par un exercice. {@code SINGLE_CHOICE}/{@code MULTIPLE_CHOICE}
 * exigent 2 à 10 choix aux identifiants uniques ; {@code FREE_TEXT}/{@code REVIEW} n'en portent
 * aucun. La validation complète (cohérence avec le corrigé) relève du domaine {@code authoring}.
 */
public record ResponseSpec(@NotNull ResponseKind kind, @Valid @Size(max = 10) List<Choice> choices) {

    public ResponseSpec {
        choices = choices == null ? List.of() : List.copyOf(choices);
    }
}
