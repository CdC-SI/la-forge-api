package ch.admin.zas.jweb.laforge.common.error;

/**
 * Détail d'un champ invalide, tel qu'exposé dans {@link Problem#violations()}.
 */
public record Violation(String field, String message) {
}
