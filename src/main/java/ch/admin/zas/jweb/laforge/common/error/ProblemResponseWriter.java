package ch.admin.zas.jweb.laforge.common.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Écrit une réponse {@code application/problem+json} directement sur la réponse HTTP, pour les
 * rejets survenant dans la chaîne de filtres Spring Security (avant que le {@code DispatcherServlet}
 * n'atteigne les {@code @ExceptionHandler} de {@link ProblemDetailsHandler}).
 */
public final class ProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ProblemCode code, String detail) throws IOException {
        var problem = Problem.of(code, code.defaultTitle(), detail, traceId());
        response.setStatus(code.defaultStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }

    private String traceId() {
        var traceId = MDC.get(TraceIdFilter.MDC_KEY);
        return traceId != null ? traceId : "unavailable";
    }
}
