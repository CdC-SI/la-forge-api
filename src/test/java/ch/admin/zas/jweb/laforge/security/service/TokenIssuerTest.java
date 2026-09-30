package ch.admin.zas.jweb.laforge.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests unitaires purs (Mockito) de {@link TokenIssuer}. */
@ExtendWith(MockitoExtension.class)
class TokenIssuerTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);
    private final LaForgeProperties properties = new LaForgeProperties(
            new LaForgeProperties.Security(
                    new LaForgeProperties.Security.Jwt("https://laforge.test", "laforge-api", Duration.ofMinutes(15), "", ""),
                    new LaForgeProperties.Security.RefreshToken(Duration.ofDays(30)),
                    new LaForgeProperties.Security.VerificationToken(Duration.ofHours(24), Duration.ofMinutes(5))),
            new LaForgeProperties.Mail("no-reply@laforge.test", "https://laforge.test/verify"),
            new LaForgeProperties.RateLimit(
                    new LaForgeProperties.RateLimit.Bucket(10, Duration.ofMinutes(1)),
                    new LaForgeProperties.RateLimit.Bucket(10, Duration.ofMinutes(1))),
            new LaForgeProperties.Http(org.springframework.util.unit.DataSize.ofMegabytes(1)));

    private TokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        tokenIssuer = new TokenIssuer(jwtEncoder, properties, clock);
    }

    @Test
    void issueAccessToken_encodeLesClaimsAttendusEtRetourneLaValeurDuJeton() {
        var account = new Account("learner@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.activate();

        var encoded = mock(Jwt.class);
        when(encoded.getTokenValue()).thenReturn("signed-jwt");
        var captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        when(jwtEncoder.encode(captor.capture())).thenReturn(encoded);

        var tokenValue = tokenIssuer.issueAccessToken(account);

        assertThat(tokenValue).isEqualTo("signed-jwt");
        var claims = captor.getValue().getClaims();
        assertThat(claims.getSubject()).isEqualTo(account.getId().toString());
        assertThat(claims.getIssuer().toString()).contains("laforge.test");
        assertThat(claims.getAudience()).containsExactly("laforge-api");
        assertThat(claims.<java.util.List<String>>getClaim("roles")).containsExactly(Role.LEARNER.name());
        assertThat(claims.<String>getClaim("displayName")).isEqualTo("Apprenant");
    }

    @Test
    void accessTokenTtl_retourneLaDureeConfiguree() {
        assertThat(tokenIssuer.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
    }
}
