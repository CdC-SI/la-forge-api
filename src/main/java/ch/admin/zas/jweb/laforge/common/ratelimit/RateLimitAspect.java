package ch.admin.zas.jweb.laforge.common.ratelimit;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.common.error.RateLimitedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Applique le quota déclaré par {@link RateLimited} avant l'exécution de la méthode annotée,
 * levant {@link RateLimitedException} (429, en-tête {@code Retry-After}) au-delà de la capacité
 * configurée sous {@code laforge.rate-limit.*}. Exécuté avant {@code IdempotencyAspect} (ordre
 * plus bas) pour ne pas consommer d'enregistrement d'idempotence sur une requête déjà rejetée.
 */
@Aspect
@Component
@Order(-10)
public class RateLimitAspect {

    private final TokenBucketLimiter limiter;
    private final LaForgeProperties properties;
    private final HttpServletRequest request;
    private final Clock clock;

    public RateLimitAspect(TokenBucketLimiter limiter, LaForgeProperties properties, HttpServletRequest request, Clock clock) {
        this.limiter = limiter;
        this.properties = properties;
        this.request = request;
        this.clock = clock;
    }

    @Around("@annotation(rateLimited)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        var bucket = bucketConfig(rateLimited.value());
        var key = rateLimited.value() + ":" + resolveKey(rateLimited.value());
        var overLimit = limiter.tryConsume(key, bucket.capacity(), bucket.refillPeriod(), clock);
        if (overLimit.isPresent()) {
            var method = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
            throw new RateLimitedException(
                    "Trop de requêtes pour '%s' ; réessayez plus tard.".formatted(method), overLimit.get());
        }
        return joinPoint.proceed();
    }

    private LaForgeProperties.RateLimit.Bucket bucketConfig(RateLimitFamily family) {
        return switch (family) {
            case AUTH -> properties.rateLimit().auth();
            case TUTOR -> properties.rateLimit().tutor();
        };
    }

    /** IP cliente pour les routes non authentifiées (auth), compte courant sinon (tuteur). */
    private String resolveKey(RateLimitFamily family) {
        if (family == RateLimitFamily.TUTOR) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getSubject();
            }
        }
        return clientIp();
    }

    private String clientIp() {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
