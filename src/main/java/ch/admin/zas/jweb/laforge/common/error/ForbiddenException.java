package ch.admin.zas.jweb.laforge.common.error;

/** Rôle insuffisant ou condition de déblocage pédagogique non remplie (403). */
public final class ForbiddenException extends ApiException {

    public ForbiddenException(String detail) {
        super(ProblemCode.FORBIDDEN, detail);
    }
}
