package ch.admin.zas.jweb.laforge.catalog.domain;

/** Verdict attendu pour un exercice de type revue de code ({@link ResponseKind#REVIEW}). */
public enum ReviewVerdict {
    APPROVE,
    REQUEST_CHANGES,
    NEED_INFORMATION
}
