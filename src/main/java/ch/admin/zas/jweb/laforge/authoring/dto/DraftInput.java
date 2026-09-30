package ch.admin.zas.jweb.laforge.authoring.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Corps de {@code POST /authoring/drafts}. {@code exerciseId} absent crée un nouvel exercice ;
 * présent, ouvre une nouvelle révision d'un exercice existant.
 */
public record DraftInput(UUID exerciseId, @NotNull @Valid ExerciseContentInput content) {
}
