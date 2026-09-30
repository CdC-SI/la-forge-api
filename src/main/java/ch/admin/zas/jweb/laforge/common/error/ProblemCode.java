package ch.admin.zas.jweb.laforge.common.error;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * Codes d'erreur applicatifs exposés dans le champ {@code code} du schéma {@code Problem} du
 * contrat. Chaque code porte son statut HTTP par défaut et un titre court en français ; le détail
 * circonstancié reste porté par l'exception qui déclenche l'erreur.
 */
public enum ProblemCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Requête invalide"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentification requise"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Accès refusé"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Ressource introuvable"),
    INVALID_STATE(HttpStatus.CONFLICT, "Transition invalide"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Conflit d'idempotence"),
    STALE_VERSION(HttpStatus.PRECONDITION_FAILED, "Version obsolète"),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Validation échouée"),
    PRECONDITION_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "Précondition requise"),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Corps de requête trop volumineux"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Trop de requêtes"),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Assistance IA indisponible"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne");

    private final HttpStatus defaultStatus;
    private final String defaultTitle;

    ProblemCode(HttpStatus defaultStatus, String defaultTitle) {
        this.defaultStatus = defaultStatus;
        this.defaultTitle = defaultTitle;
    }

    public HttpStatus defaultStatus() {
        return defaultStatus;
    }

    public String defaultTitle() {
        return defaultTitle;
    }

    /** URN stable identifiant le type d'erreur, conforme au format {@code urn:la-forge:problem:*}. */
    public URI type() {
        return URI.create("urn:la-forge:problem:" + name().toLowerCase().replace('_', '-'));
    }
}
