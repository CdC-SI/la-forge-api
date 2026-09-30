package ch.admin.zas.jweb.laforge.catalog.domain;

/** Format de réponse attendu pour un exercice ; détermine la variante d'{@code Answer} valide. */
public enum ResponseKind {
    FREE_TEXT,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    REVIEW
}
