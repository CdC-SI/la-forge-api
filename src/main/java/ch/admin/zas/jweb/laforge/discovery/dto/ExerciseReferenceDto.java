package ch.admin.zas.jweb.laforge.discovery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Référence à une version publiée d'exercice, utilisée pour lier une fiche de veille. */
public record ExerciseReferenceDto(@NotNull UUID exerciseId, @Min(1) int version) {
}
