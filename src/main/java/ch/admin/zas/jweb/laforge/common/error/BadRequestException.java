package ch.admin.zas.jweb.laforge.common.error;

/** Requête syntaxiquement invalide (400). */
public final class BadRequestException extends ApiException {

    public BadRequestException(String detail) {
        super(ProblemCode.BAD_REQUEST, detail);
    }
}
