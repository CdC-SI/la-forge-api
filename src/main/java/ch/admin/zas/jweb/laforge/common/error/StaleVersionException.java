package ch.admin.zas.jweb.laforge.common.error;

/** L'ETag fort fourni via {@code If-Match} est obsolète (412). */
public final class StaleVersionException extends ApiException {

    public StaleVersionException(String detail) {
        super(ProblemCode.STALE_VERSION, detail);
    }
}
