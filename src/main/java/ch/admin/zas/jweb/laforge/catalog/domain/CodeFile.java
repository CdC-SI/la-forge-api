package ch.admin.zas.jweb.laforge.catalog.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Fichier fourni à l'apprenant dans l'énoncé (code source, extrait de log, diff, test...). Le
 * chemin est logique uniquement : jamais résolu sur un système de fichiers réel.
 */
public record CodeFile(
        @NotBlank @Size(max = 240) String path,
        @NotBlank @Size(max = 40) String language,
        @Size(max = 50000) String content,
        @NotNull CodeFileKind kind) {
}
