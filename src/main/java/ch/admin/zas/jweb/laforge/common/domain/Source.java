package ch.admin.zas.jweb.laforge.common.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** Source documentaire citée par un corrigé, une fiche de veille ou une réponse du tuteur. */
public record Source(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2048) String url,
        @NotNull OffsetDateTime accessedAt) {
}
