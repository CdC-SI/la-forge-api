package ch.admin.zas.jweb.laforge.common.error;

/** Ressource inexistante ou hors périmètre de l'appelant (404). */
public final class NotFoundException extends ApiException {

    public NotFoundException(String detail) {
        super(ProblemCode.NOT_FOUND, detail);
    }
}
