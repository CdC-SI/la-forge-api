package ch.admin.zas.jweb.laforge.common.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Limiteur de débit en mémoire à seau à jetons : chaque clé dispose de {@code capacity} jetons qui
 * se rechargent linéairement jusqu'à ce plafond sur la durée {@code refillPeriod}. Convient à une
 * instance unique (v1) ; une mise à l'échelle multi-instance nécessiterait un magasin partagé
 * (ex. Redis) hors périmètre v1.
 */
@Component
public class TokenBucketLimiter {

    private record State(double tokens, Instant lastRefill) {
    }

    private final ConcurrentHashMap<String, AtomicReference<State>> buckets = new ConcurrentHashMap<>();

    /**
     * Tente de consommer un jeton pour la clé donnée.
     *
     * @return {@link Optional#empty()} si autorisé, sinon le délai minimal avant nouvel essai
     */
    public Optional<Duration> tryConsume(String key, int capacity, Duration refillPeriod, Clock clock) {
        var ref = buckets.computeIfAbsent(key, k -> new AtomicReference<>(new State(capacity, clock.instant())));
        var refillRatePerMillis = capacity / (double) refillPeriod.toMillis();

        while (true) {
            var current = ref.get();
            var now = clock.instant();
            var elapsedMillis = Duration.between(current.lastRefill(), now).toMillis();
            var refilled = Math.min(capacity, current.tokens() + elapsedMillis * refillRatePerMillis);

            if (refilled >= 1.0) {
                var updated = new State(refilled - 1.0, now);
                if (ref.compareAndSet(current, updated)) {
                    return Optional.empty();
                }
                continue;
            }

            var updated = new State(refilled, now);
            ref.compareAndSet(current, updated);
            var missingTokens = 1.0 - refilled;
            var waitMillis = (long) Math.ceil(missingTokens / refillRatePerMillis);
            return Optional.of(Duration.ofMillis(Math.max(waitMillis, 1)));
        }
    }
}
