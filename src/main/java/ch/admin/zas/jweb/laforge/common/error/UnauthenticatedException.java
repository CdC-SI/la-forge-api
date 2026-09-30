package ch.admin.zas.jweb.laforge.common.error;

/** Jeton absent, expiré ou invalide (401). */
public final class UnauthenticatedException extends ApiException {

    public UnauthenticatedException(String detail) {
        super(ProblemCode.UNAUTHENTICATED, detail);
    }
}
