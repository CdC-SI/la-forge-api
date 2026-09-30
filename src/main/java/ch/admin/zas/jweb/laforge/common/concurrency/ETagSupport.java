package ch.admin.zas.jweb.laforge.common.concurrency;

import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import ch.admin.zas.jweb.laforge.common.error.PreconditionRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Support transverse pour la concurrence optimiste exposée via {@code If-Match} / {@code ETag}
 * fort (contrat : uniquement les mutations de brouillon d'exercice — {@code /authoring/drafts/**}).
 * L'ETag est la révision {@code @Version} de l'entité, encodée en chaîne entre guillemets. Absent
 * sur une mutation : 428 ({@link PreconditionRequiredException}) ; syntaxiquement invalide : 400 ;
 * valide mais périmé : 412, levé par le domaine via {@code StaleVersionException} au moment de la
 * transition (l'ETag seul ne peut pas savoir si la révision est encore actuelle).
 *
 * <p>Composant volontairement simple : un seul contrôleur en dépend en v1. S'il devait couvrir
 * plusieurs ressources versionnées, cette classe migrerait vers un {@code HandlerInterceptor} ou
 * une résolution d'argument dédiée plutôt que d'être appelée explicitement par chaque méthode.
 */
public final class ETagSupport {

    private ETagSupport() {
    }

    /** Extrait la révision attendue d'un en-tête {@code If-Match} fort, tel qu'exigé par le contrat. */
    public static int requireRevision(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new PreconditionRequiredException("L'en-tête If-Match est requis pour cette opération.");
        }
        try {
            return Integer.parseInt(ifMatch.replace("\"", "").trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("L'en-tête If-Match doit contenir un ETag fort numérique.");
        }
    }

    /** Encode une révision en ETag fort (entre guillemets, sans joker). */
    public static String toETag(int revision) {
        return "\"" + revision + "\"";
    }

    /** Construit une réponse portant l'ETag fort dérivé de la révision courante de la ressource. */
    public static <T> ResponseEntity<T> withETag(HttpStatus status, int revision, T body) {
        return ResponseEntity.status(status).eTag(toETag(revision)).body(body);
    }
}
