package ch.admin.zas.jweb.laforge.common.web;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.common.error.ProblemCode;
import ch.admin.zas.jweb.laforge.common.error.ProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Refuse tôt les corps de requête déclarés (en-tête {@code Content-Length}) au-delà de
 * {@code laforge.http.max-request-body-size}, et enveloppe le flux d'entrée pour interrompre
 * également les corps sans longueur déclarée (transfert fragmenté) qui dépasseraient la limite en
 * cours de lecture — voir {@link SizeLimitedHttpServletRequest}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MaxRequestBodySizeFilter extends OncePerRequestFilter {

    private final long maxBytes;
    private final ProblemResponseWriter problemResponseWriter;

    public MaxRequestBodySizeFilter(LaForgeProperties properties, ObjectMapper objectMapper) {
        this.maxBytes = properties.http().maxRequestBodySize().toBytes();
        this.problemResponseWriter = new ProblemResponseWriter(objectMapper);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var declaredLength = request.getContentLengthLong();
        if (declaredLength > maxBytes) {
            problemResponseWriter.write(
                    response,
                    ProblemCode.PAYLOAD_TOO_LARGE,
                    "Le corps de la requête dépasse la taille maximale autorisée de %d octets.".formatted(maxBytes));
            return;
        }
        chain.doFilter(new SizeLimitedHttpServletRequest(request, maxBytes), response);
    }
}
