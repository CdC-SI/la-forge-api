package ch.admin.zas.jweb.laforge.common.error;

/** Même clé d'idempotence rejouée avec un corps différent (409). */
public final class IdempotencyConflictException extends ApiException {

    public IdempotencyConflictException(String detail) {
        super(ProblemCode.IDEMPOTENCY_CONFLICT, detail);
    }
}
