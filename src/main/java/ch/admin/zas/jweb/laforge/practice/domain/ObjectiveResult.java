package ch.admin.zas.jweb.laforge.practice.domain;

/**
 * Résultat de la correction automatique, figé à la soumission. Déterministe uniquement pour les
 * réponses à choix ; les réponses libres et les revues de code ne sont jamais notées automatiquement.
 */
public enum ObjectiveResult {
    CORRECT,
    INCORRECT,
    NOT_AUTO_GRADED
}
