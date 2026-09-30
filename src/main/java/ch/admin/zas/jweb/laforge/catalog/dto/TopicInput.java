package ch.admin.zas.jweb.laforge.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /topics}. L'identifiant est toujours généré par le serveur. */
public record TopicInput(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
        @NotBlank @Size(max = 200) String label) {
}
