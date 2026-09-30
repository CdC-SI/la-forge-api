package ch.admin.zas.jweb.laforge.practice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Corps de {@code POST /attempts}. {@code challengeId} et {@code reviewItemId} sont mutuellement
 * exclusifs, vérifié par le service (contrainte non exprimable en Bean Validation simple).
 */
public record CreateAttemptInput(
        @NotNull UUID exerciseId, @Min(1) int exerciseVersion, UUID challengeId, UUID reviewItemId) {
}
