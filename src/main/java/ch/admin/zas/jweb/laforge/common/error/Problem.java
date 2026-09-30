package ch.admin.zas.jweb.laforge.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Corps d'erreur {@code application/problem+json}, conforme au schéma {@code Problem} du contrat
 * OpenAPI. Les champs absents (ex. {@code instance}, {@code violations} vide) sont omis de la
 * sérialisation JSON.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Problem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        ProblemCode code,
        String traceId,
        List<Violation> violations) {

    public Problem {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public static Problem of(ProblemCode code, String title, String detail, String traceId) {
        return new Problem(code.type().toString(), title, code.defaultStatus().value(), detail, null, code, traceId,
                List.of());
    }

    public static Problem of(ProblemCode code, String title, String detail, String traceId, List<Violation> violations) {
        return new Problem(code.type().toString(), title, code.defaultStatus().value(), detail, null, code, traceId,
                violations);
    }
}
