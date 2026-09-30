package ch.admin.zas.jweb.laforge.common.idempotency;

import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import ch.admin.zas.jweb.laforge.common.error.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import tools.jackson.databind.ObjectMapper;

/**
 * Intercepte les méthodes de contrôleur annotées {@link Idempotent} pour appliquer la
 * déduplication décrite par le contrat. La clé d'idempotence, obligatoire, est lue depuis
 * l'en-tête {@code Idempotency-Key} ; le corps de la requête (paramètre {@code @RequestBody}, s'il
 * existe) est haché en SHA-256 pour détecter une réutilisation avec un corps différent. En cas de
 * répétition avec un corps identique, la réponse d'origine est retournée sans ré-exécuter la
 * méthode ; en cas d'échec, l'enregistrement est libéré pour permettre un nouvel essai immédiat
 * avec la même clé.
 */
@Aspect
@Component
@Order(0)
public class IdempotencyAspect {

    private static final String HEADER = "Idempotency-Key";

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public IdempotencyAspect(IdempotencyService idempotencyService, ObjectMapper objectMapper, HttpServletRequest request) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Around("@annotation(ch.admin.zas.jweb.laforge.common.idempotency.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        var idempotencyKey = requireIdempotencyKey();
        var accountId = requireAccountId();
        var httpMethod = request.getMethod();
        var path = request.getRequestURI();
        var method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        var requestHash = hashRequestBody(joinPoint, method);

        var cached = idempotencyService.begin(idempotencyKey, accountId, httpMethod, path, requestHash);
        if (cached.isPresent()) {
            return replay(cached.get(), method);
        }

        try {
            var result = joinPoint.proceed();
            var envelope = Envelope.capture(result, successStatus(method), objectMapper);
            idempotencyService.complete(
                    idempotencyKey, accountId, httpMethod, path, envelope.status(), objectMapper.writeValueAsString(envelope));
            return result;
        } catch (Throwable failure) {
            idempotencyService.release(idempotencyKey, accountId, httpMethod, path);
            throw failure;
        }
    }

    private Object replay(IdempotencyService.CachedResponse cached, Method method) {
        var envelope = objectMapper.readValue(cached.body(), Envelope.class);
        Object body = null;
        if (envelope.bodyClass() != null) {
            try {
                body = objectMapper.readValue(envelope.bodyJson(), Class.forName(envelope.bodyClass()));
            } catch (ClassNotFoundException notFound) {
                throw new IllegalStateException("Type de corps mis en cache introuvable : " + envelope.bodyClass(), notFound);
            }
        }
        if (method.getReturnType() == ResponseEntity.class) {
            var builder = ResponseEntity.status(cached.status());
            envelope.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
            return builder.body(body);
        }
        return body;
    }

    /** Enveloppe sérialisable capturant statut, en-têtes et corps d'une réponse, y compris pour {@link ResponseEntity}. */
    private record Envelope(int status, Map<String, List<String>> headers, String bodyClass, String bodyJson) {

        static Envelope capture(Object result, int defaultStatus, ObjectMapper objectMapper) {
            if (result instanceof ResponseEntity<?> entity) {
                Object body = entity.getBody();
                Map<String, List<String>> headers = new java.util.LinkedHashMap<>();
                entity.getHeaders().forEach(headers::put);
                return new Envelope(
                        entity.getStatusCode().value(),
                        headers,
                        body != null ? body.getClass().getName() : null,
                        body != null ? objectMapper.writeValueAsString(body) : null);
            }
            return new Envelope(
                    defaultStatus,
                    Map.of(),
                    result != null ? result.getClass().getName() : null,
                    result != null ? objectMapper.writeValueAsString(result) : null);
        }
    }

    private UUID requireIdempotencyKey() {
        var header = request.getHeader(HEADER);
        if (header == null || header.isBlank()) {
            throw new BadRequestException("L'en-tête Idempotency-Key est obligatoire pour cette opération.");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException invalid) {
            throw new BadRequestException("L'en-tête Idempotency-Key doit être un UUID.");
        }
    }

    private UUID requireAccountId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthenticatedException("Authentification requise.");
        }
        return UUID.fromString(jwt.getSubject());
    }

    private String hashRequestBody(ProceedingJoinPoint joinPoint, Method method) {
        var parameters = method.getParameters();
        var args = joinPoint.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                return sha256(objectMapper.writeValueAsString(args[i]));
            }
        }
        return sha256("");
    }

    private int successStatus(Method method) {
        var responseStatus = AnnotationUtils.findAnnotation(method, ResponseStatus.class);
        return responseStatus != null ? responseStatus.code().value() : 200;
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponible.", impossible);
        }
    }
}
