package ch.admin.zas.jweb.laforge.common.error;

/** Transition ou opération interdite dans l'état courant de la ressource (409). */
public final class InvalidStateException extends ApiException {

    public InvalidStateException(String detail) {
        super(ProblemCode.INVALID_STATE, detail);
    }
}
