package ch.admin.zas.jweb.laforge.security.config;

import ch.admin.zas.jweb.laforge.common.error.ProblemCode;
import ch.admin.zas.jweb.laforge.common.error.ProblemResponseWriter;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

/**
 * Chaîne de filtres HTTP : API sans état (aucune session, aucun CSRF), authentification par
 * porteur JWT (RS256) sur toutes les routes sauf {@code /auth/**} et la sonde de santé, réponses
 * d'erreur au format {@code application/problem+json} conforme au contrat. Les en-têtes de
 * durcissement ({@code Cache-Control}, {@code X-Frame-Options}, etc.) sont désactivés ici et pris
 * en charge uniformément par {@link ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, ObjectMapper objectMapper)
            throws Exception {
        var problemResponseWriter = new ProblemResponseWriter(objectMapper);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> problemResponseWriter.write(
                                response, ProblemCode.UNAUTHENTICATED, "Jeton absent, expiré ou invalide."))
                        .accessDeniedHandler((request, response, ex) -> problemResponseWriter.write(
                                response, ProblemCode.FORBIDDEN, "Rôle insuffisant ou condition de déblocage non remplie.")));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        Set<String> recognized = Arrays.stream(Role.values()).map(role -> "ROLE_" + role.name())
                .collect(Collectors.toSet());
        converter.setJwtGrantedAuthoritiesConverter(jwt -> authoritiesConverter.convert(jwt).stream()
                .filter(authority -> recognized.contains(authority.getAuthority())).toList());
        return converter;
    }
}
