package ch.admin.zas.jweb.laforge.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ajoute les en-têtes de durcissement transverses à toute réponse de l'API : {@code Cache-Control}
 * empêchant toute mise en cache intermédiaire (données pédagogiques et de progression, jamais
 * publiques au sens HTTP même pour les lectures non authentifiées du catalogue), et les en-têtes
 * standards de protection du navigateur ({@code X-Content-Type-Options}, {@code X-Frame-Options},
 * {@code Referrer-Policy}) — une API JSON pure n'est jamais rendue dans un cadre ni référencée par
 * un lien à faire suivre, mais ces en-têtes coûtent peu et protègent les clients mal configurés.
 * Appliqué uniformément par simplicité plutôt que conditionné à la présence d'un jeton, ce qui
 * reste conforme à l'exigence minimale du contrat (« Cache-Control: private, no-store sur les
 * réponses privées ») sans risque de l'oublier sur une route nouvellement ajoutée.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        chain.doFilter(request, response);
    }
}
