package ch.admin.zas.jweb.laforge.common.error;

import java.util.List;

/** Données ou contraintes métier invalides ; {@link #violations()} détaille les champs (422). */
public final class ValidationFailedException extends ApiException {

    public ValidationFailedException(String detail, List<Violation> violations) {
        super(ProblemCode.VALIDATION_FAILED, detail, violations);
    }

    public ValidationFailedException(String detail) {
        this(detail, List.of());
    }
}
