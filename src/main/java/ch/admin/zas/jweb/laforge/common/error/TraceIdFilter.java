package ch.admin.zas.jweb.laforge.common.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

/**
 * Attribue un identifiant de traçage à chaque requête (repris de l'en-tête entrant
 * {@code X-Trace-Id} s'il est présent, sinon généré), le place dans le MDC pour la corrélation des
 * logs et le renvoie dans l'en-tête de réponse. {@link ProblemDetailsHandler} le reprend tel quel
 * dans le champ {@code traceId} du corps d'erreur.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends GenericFilterBean {

    public static final String MDC_KEY = "traceId";
    static final String HEADER_NAME = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var incoming = request instanceof jakarta.servlet.http.HttpServletRequest http
                ? http.getHeader(HEADER_NAME)
                : null;
        var traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
        MDC.put(MDC_KEY, traceId);
        try {
            if (response instanceof HttpServletResponse http) {
                http.setHeader(HEADER_NAME, traceId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
