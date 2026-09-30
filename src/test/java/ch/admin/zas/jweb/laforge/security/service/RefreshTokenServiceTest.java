package ch.admin.zas.jweb.laforge.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.common.error.UnauthenticatedException;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.RefreshToken;
import ch.admin.zas.jweb.laforge.security.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests unitaires purs (Mockito) de {@link RefreshTokenService} : émission, rotation et révocation
 * de famille en cas de rejeu d'un jeton déjà consommé.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;
    @Mock
    private SecureTokenFactory tokenFactory;

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);
    private final LaForgeProperties properties = new LaForgeProperties(
            new LaForgeProperties.Security(
                    new LaForgeProperties.Security.Jwt("issuer", "audience", Duration.ofMinutes(15), "", ""),
                    new LaForgeProperties.Security.RefreshToken(Duration.ofDays(30)),
                    new LaForgeProperties.Security.VerificationToken(Duration.ofHours(24), Duration.ofMinutes(5))),
            new LaForgeProperties.Mail("no-reply@laforge.test", "https://laforge.test/verify"),
            new LaForgeProperties.RateLimit(
                    new LaForgeProperties.RateLimit.Bucket(10, Duration.ofMinutes(1)),
                    new LaForgeProperties.RateLimit.Bucket(10, Duration.ofMinutes(1))),
            new LaForgeProperties.Http(org.springframework.util.unit.DataSize.ofMegabytes(1)));

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, tokenFactory, properties, clock);
    }

    private static Account newAccount() {
        var account = new Account("learner@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    @Test
    void issueNewFamily_creeUnJetonActifDansUneNouvelleFamille() {
        var account = newAccount();
        when(tokenFactory.generateOpaqueToken()).thenReturn("raw-token");
        when(tokenFactory.hash("raw-token")).thenReturn("hashed-token");

        var issued = service.issueNewFamily(account);

        assertThat(issued.rawToken()).isEqualTo("raw-token");
        assertThat(issued.account()).isEqualTo(account);
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void rotate_jetonInconnu_declencheUnauthenticatedException() {
        when(tokenFactory.hash("raw")).thenReturn("hash");
        when(repository.findByTokenHash("hash")).thenReturn(Optional.empty());

        assertThrows(UnauthenticatedException.class, () -> service.rotate("raw"));
    }

    @Test
    void rotate_jetonActif_leRevoqueEtEnEmetUnNouveauDansLaMemeFamille() {
        var account = newAccount();
        var familyId = UUID.randomUUID();
        var current = new RefreshToken(account, "hash", familyId, OffsetDateTime.now(clock).plusDays(1));
        when(tokenFactory.hash("raw")).thenReturn("hash");
        when(repository.findByTokenHash("hash")).thenReturn(Optional.of(current));
        when(tokenFactory.generateOpaqueToken()).thenReturn("raw-2");
        when(tokenFactory.hash("raw-2")).thenReturn("hash-2");

        var issued = service.rotate("raw");

        assertThat(current.getRevokedAt()).isEqualTo(OffsetDateTime.now(clock));
        assertThat(issued.rawToken()).isEqualTo("raw-2");
        verify(repository, never()).findByFamilyId(any());
    }

    @Test
    void rotate_jetonDejaRevoque_revoqueTouteLaFamilleEtDeclencheUnauthenticatedException() {
        var account = newAccount();
        var familyId = UUID.randomUUID();
        var revokedToken = new RefreshToken(account, "hash", familyId, OffsetDateTime.now(clock).plusDays(1));
        revokedToken.revoke(OffsetDateTime.now(clock).minusMinutes(1));
        var siblingToken = new RefreshToken(account, "hash-sibling", familyId, OffsetDateTime.now(clock).plusDays(1));
        when(tokenFactory.hash("raw")).thenReturn("hash");
        when(repository.findByTokenHash("hash")).thenReturn(Optional.of(revokedToken));
        when(repository.findByFamilyId(familyId)).thenReturn(List.of(revokedToken, siblingToken));

        assertThrows(UnauthenticatedException.class, () -> service.rotate("raw"));

        assertThat(siblingToken.getRevokedAt()).isEqualTo(OffsetDateTime.now(clock));
    }

    @Test
    void rotate_jetonExpire_revoqueTouteLaFamilleEtDeclencheUnauthenticatedException() {
        var account = newAccount();
        var familyId = UUID.randomUUID();
        var expiredToken = new RefreshToken(account, "hash", familyId, OffsetDateTime.now(clock).minusMinutes(1));
        when(tokenFactory.hash("raw")).thenReturn("hash");
        when(repository.findByTokenHash("hash")).thenReturn(Optional.of(expiredToken));
        when(repository.findByFamilyId(familyId)).thenReturn(List.of(expiredToken));

        assertThrows(UnauthenticatedException.class, () -> service.rotate("raw"));
    }

    @Test
    void revoke_jetonConnu_leMarqueRevoque() {
        var account = newAccount();
        var token = new RefreshToken(account, "hash", UUID.randomUUID(), OffsetDateTime.now(clock).plusDays(1));
        when(tokenFactory.hash("raw")).thenReturn("hash");
        when(repository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        service.revoke("raw");

        assertThat(token.getRevokedAt()).isEqualTo(OffsetDateTime.now(clock));
    }

    @Test
    void revoke_jetonInconnu_neFaitRienSansException() {
        when(tokenFactory.hash("raw")).thenReturn("hash");
        when(repository.findByTokenHash("hash")).thenReturn(Optional.empty());

        service.revoke("raw");

        verify(repository, times(0)).save(any());
    }
}
