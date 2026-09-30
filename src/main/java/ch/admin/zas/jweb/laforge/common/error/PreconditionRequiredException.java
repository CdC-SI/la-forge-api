package ch.admin.zas.jweb.laforge.common.error;

/** L'en-tête {@code If-Match} est requis mais absent (428). */
public final class PreconditionRequiredException extends ApiException {

    public PreconditionRequiredException(String detail) {
        super(ProblemCode.PRECONDITION_REQUIRED, detail);
    }
}
