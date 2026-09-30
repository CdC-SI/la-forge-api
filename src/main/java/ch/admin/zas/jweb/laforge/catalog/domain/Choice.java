package ch.admin.zas.jweb.laforge.catalog.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Option proposée pour une réponse à choix (unique ou multiple). */
public record Choice(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String id,
        @NotBlank @Size(max = 2000) String label) {
}
