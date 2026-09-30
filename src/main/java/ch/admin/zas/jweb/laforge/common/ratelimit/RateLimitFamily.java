package ch.admin.zas.jweb.laforge.common.ratelimit;

/**
 * Famille de quota partageant une même configuration {@code laforge.rate-limit.*} (contrat :
 * limitation de débit sur les routes d'authentification et sur le tuteur assisté par IA).
 */
public enum RateLimitFamily {
    AUTH,
    TUTOR
}
