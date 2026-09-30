package ch.admin.zas.jweb.laforge.common.error;

import java.time.Duration;

/** Assistance IA désactivée ou indisponible ; aucun échange partiel n'est enregistré (503). */
public final class AiUnavailableException extends ApiException {

    private final Duration retryAfter;

    public AiUnavailableException(String detail, Duration retryAfter) {
        super(ProblemCode.AI_UNAVAILABLE, detail);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
