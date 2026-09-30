package ch.admin.zas.jweb.laforge.common.error;

import java.time.Duration;

/** Quota dépassé ; {@link #retryAfter()} indique le délai minimal avant nouvel essai (429). */
public final class RateLimitedException extends ApiException {

    private final Duration retryAfter;

    public RateLimitedException(String detail, Duration retryAfter) {
        super(ProblemCode.RATE_LIMITED, detail);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
