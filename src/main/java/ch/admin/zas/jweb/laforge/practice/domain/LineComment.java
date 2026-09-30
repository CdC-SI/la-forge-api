package ch.admin.zas.jweb.laforge.practice.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Commentaire ancré à une ligne précise d'un {@code CodeFile} de l'énoncé, dans une revue. */
public record LineComment(
        @NotBlank @Size(max = 240) String filePath,
        @Min(1) int line,
        @NotNull CommentSeverity severity,
        @NotBlank @Size(max = 2000) String comment) {
}
