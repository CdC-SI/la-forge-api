package ch.admin.zas.jweb.laforge.catalog.domain;

/** Indice progressif révélable pendant une tentative (niveau 1 à 5). */
public record Hint(int level, String markdown) {
}
