package ch.admin.zas.jweb.laforge.security.service;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Émet les JWT d'accès signés RS256. L'utilisateur courant sera toujours déduit du claim
 * {@code sub} (identifiant de compte) par le serveur de ressources, jamais du corps de requête.
 */
@Service
public class TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final LaForgeProperties properties;
    private final Clock clock;

    public TokenIssuer(JwtEncoder jwtEncoder, LaForgeProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issueAccessToken(Account account) {
        var jwtProperties = properties.security().jwt();
        var now = Instant.now(clock);
        var claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .audience(List.of(jwtProperties.audience()))
                .subject(account.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTokenTtl()))
                .claim("roles", account.getRoles().stream().map(Role::name).toList())
                .claim("displayName", account.getDisplayName())
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Duration accessTokenTtl() {
        return properties.security().jwt().accessTokenTtl();
    }
}
