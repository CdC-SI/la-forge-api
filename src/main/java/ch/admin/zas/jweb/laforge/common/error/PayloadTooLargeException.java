package ch.admin.zas.jweb.laforge.common.error;

/** Corps de requête excédant la taille maximale acceptée par l'application (413). */
public final class PayloadTooLargeException extends ApiException {

    public PayloadTooLargeException(String detail) {
        super(ProblemCode.PAYLOAD_TOO_LARGE, detail);
    }
}
