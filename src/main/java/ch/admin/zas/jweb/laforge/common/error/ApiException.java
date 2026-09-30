package ch.admin.zas.jweb.laforge.common.error;

import java.util.List;

/**
 * Racine scellée des exceptions applicatives traduites en {@link Problem}. Chaque sous-type fixe
 * son {@link ProblemCode} (et donc son statut HTTP par défaut) ; le message porté par
 * l'exception devient le champ {@code detail} de la réponse.
 */
public abstract sealed class ApiException extends RuntimeException
        permits BadRequestException, UnauthenticatedException, ForbiddenException, NotFoundException,
        InvalidStateException, IdempotencyConflictException, StaleVersionException, ValidationFailedException,
        PreconditionRequiredException, RateLimitedException, AiUnavailableException, PayloadTooLargeException {

    private final ProblemCode code;
    private final List<Violation> violations;

    protected ApiException(ProblemCode code, String detail) {
        this(code, detail, List.of());
    }

    protected ApiException(ProblemCode code, String detail, List<Violation> violations) {
        super(detail);
        this.code = code;
        this.violations = List.copyOf(violations);
    }

    public ProblemCode code() {
        return code;
    }

    public List<Violation> violations() {
        return violations;
    }
}
