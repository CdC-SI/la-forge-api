package ch.admin.zas.jweb.laforge.collective.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Corps de {@code POST /challenges}. Échéance strictement future, au maximum dans 30 jours (vérifié par le service). */
public record ChallengeInput(
        @NotBlank @Size(max = 200) String title,
        @NotNull UUID exerciseId,
        @Min(1) int exerciseVersion,
        @NotNull @Future OffsetDateTime closesAt) {
}
