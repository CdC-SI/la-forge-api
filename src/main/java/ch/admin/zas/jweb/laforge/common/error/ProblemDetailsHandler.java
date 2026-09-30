package ch.admin.zas.jweb.laforge.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduit toute exception levée par les contrôleurs en réponse {@code application/problem+json}
 * conforme au schéma {@code Problem} du contrat. Point d'entrée unique pour la cohérence des
 * codes d'erreur : les services et contrôleurs lèvent des {@link ApiException}, jamais de statut
 * HTTP construit à la main.
 */
@RestControllerAdvice
public class ProblemDetailsHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Problem> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        return respond(ProblemCode.BAD_REQUEST, "Un paramètre de la requête est invalide.",
                List.of(new Violation(exception.getName(), "La valeur ne respecte pas le format attendu.")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Problem> handleMissingResource(NoResourceFoundException exception) {
        return respond(ProblemCode.NOT_FOUND, "Ressource introuvable.", List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Problem> handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        return respond(ProblemCode.STALE_VERSION, "La ressource a été modifiée par une autre requête.", List.of());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Problem> handleApiException(ApiException exception, WebRequest request) {
        var problem = Problem.of(exception.code(), exception.code().defaultTitle(), exception.getMessage(),
                traceId(), exception.violations());
        var headers = new HttpHeaders();
        if (exception instanceof RateLimitedException rateLimited && rateLimited.retryAfter() != null) {
            headers.add(HttpHeaders.RETRY_AFTER, Long.toString(rateLimited.retryAfter().toSeconds()));
        }
        if (exception instanceof AiUnavailableException aiUnavailable && aiUnavailable.retryAfter() != null) {
            headers.add(HttpHeaders.RETRY_AFTER, Long.toString(aiUnavailable.retryAfter().toSeconds()));
        }
        if (exception.code() == ProblemCode.UNAUTHENTICATED) {
            headers.add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return ResponseEntity.status(exception.code().defaultStatus())
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidation(MethodArgumentNotValidException exception) {
        var violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toViolation)
                .toList();
        return respond(ProblemCode.VALIDATION_FAILED, "Le corps de la requête ne respecte pas les contraintes attendues.",
                violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Problem> handleConstraintViolation(ConstraintViolationException exception) {
        var violations = exception.getConstraintViolations().stream()
                .map(violation -> new Violation(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return respond(ProblemCode.BAD_REQUEST, "Un ou plusieurs paramètres de la requête sont invalides.", violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return respond(ProblemCode.BAD_REQUEST, "Le corps de la requête est illisible ou contient des propriétés inconnues.",
                List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Problem> handleMissingHeader(MissingRequestHeaderException exception) {
        if ("If-Match".equals(exception.getHeaderName())) {
            return respond(ProblemCode.PRECONDITION_REQUIRED, "L'en-tête If-Match est requis pour cette mutation.",
                    List.of());
        }
        return respond(ProblemCode.BAD_REQUEST, "L'en-tête %s est requis.".formatted(exception.getHeaderName()), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Problem> handleAccessDenied(AccessDeniedException exception) {
        return respond(ProblemCode.FORBIDDEN, "Rôle insuffisant ou condition de déblocage non remplie.", List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Problem> handleAuthentication(AuthenticationException exception) {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Problem.of(ProblemCode.UNAUTHENTICATED, ProblemCode.UNAUTHENTICATED.defaultTitle(),
                        "Jeton absent, expiré ou invalide.", traceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleUnexpected(Exception exception) {
        log.error("Erreur interne inattendue [traceId={}]", traceId(), exception);
        return respond(ProblemCode.INTERNAL_ERROR, "Une erreur inattendue est survenue.", List.of());
    }

    private ResponseEntity<Problem> respond(ProblemCode code, String detail, List<Violation> violations) {
        var problem = Problem.of(code, code.defaultTitle(), detail, traceId(), violations);
        return ResponseEntity.status(code.defaultStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private Violation toViolation(FieldError error) {
        return new Violation(error.getField(), error.getDefaultMessage());
    }

    private String traceId() {
        var traceId = MDC.get(TraceIdFilter.MDC_KEY);
        return traceId != null ? traceId : "unavailable";
    }
}
