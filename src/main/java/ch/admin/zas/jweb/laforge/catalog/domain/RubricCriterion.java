package ch.admin.zas.jweb.laforge.catalog.domain;

/** Critère d'une grille de correction, avec son importance et son explication. */
public record RubricCriterion(String id, String label, RubricImportance importance, String explanationMarkdown) {
}
