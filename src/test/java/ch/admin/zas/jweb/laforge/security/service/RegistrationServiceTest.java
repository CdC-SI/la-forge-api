package ch.admin.zas.jweb.laforge.security.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.VerificationToken;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.security.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests unitaires purs (Mockito) de {@link RegistrationService}. */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecureTokenFactory tokenFactory;
    @Mock
    private ApplicationEventPublisher eventPublisher;

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

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(
                accountRepository, verificationTokenRepository, passwordEncoder, tokenFactory, properties, clock, eventPublisher);
    }

    @Test
    void register_adresseDejaExistante_neCreeAucunCompteEtNeReveleRienAuAppelant() {
        when(accountRepository.existsByEmail("deja@example.com")).thenReturn(true);

        service.register("deja@example.com", "secret", "Alice");

        verify(accountRepository, never()).save(any());
        verify(tokenFactory, never()).generateOpaqueToken();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void register_nouvelleAdresse_creeLeCompteEtEmetUnJetonDeVerification() {
        when(accountRepository.existsByEmail("nouveau@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(tokenFactory.generateOpaqueToken()).thenReturn("raw-token");
        when(tokenFactory.hash("raw-token")).thenReturn("hashed-token");

        service.register("Nouveau@Example.com ", "secret", "Bob");

        verify(accountRepository).save(any(Account.class));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(eventPublisher).publishEvent(any(VerificationTokenIssuedEvent.class));
    }

    @Test
    void resendVerification_compteInexistant_neFaitRien() {
        when(accountRepository.findByEmail("absent@example.com")).thenReturn(java.util.Optional.empty());

        service.resendVerification("absent@example.com");

        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void resendVerification_compteDejaActive_neReemetPasDeJeton() {
        var account = new Account("actif@example.com", "hash", "Carole");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.activate();
        when(accountRepository.findByEmail("actif@example.com")).thenReturn(java.util.Optional.of(account));

        service.resendVerification("actif@example.com");

        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void resendVerification_dansLeDelaiDeCooldown_neReemetPasDeJeton() {
        var account = new Account("attente@example.com", "hash", "Denis");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        when(accountRepository.findByEmail("attente@example.com")).thenReturn(java.util.Optional.of(account));
        var recentToken = new VerificationToken(account, "hash", OffsetDateTime.now(clock).plusHours(1));
        ReflectionTestUtils.setField(recentToken, "createdAt", OffsetDateTime.now(clock).minusMinutes(1));
        when(verificationTokenRepository.findByAccountAndConsumedAtIsNullAndExpiresAtAfter(account, OffsetDateTime.now(clock)))
                .thenReturn(List.of(recentToken));

        service.resendVerification("attente@example.com");

        verify(verificationTokenRepository, never()).save(any());
    }

    @Test
    void resendVerification_horsCooldown_reemetUnNouveauJeton() {
        var account = new Account("attente2@example.com", "hash", "Eve");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        when(accountRepository.findByEmail("attente2@example.com")).thenReturn(java.util.Optional.of(account));
        when(verificationTokenRepository.findByAccountAndConsumedAtIsNullAndExpiresAtAfter(account, OffsetDateTime.now(clock)))
                .thenReturn(List.of());
        when(tokenFactory.generateOpaqueToken()).thenReturn("raw-token-2");
        when(tokenFactory.hash("raw-token-2")).thenReturn("hashed-token-2");

        service.resendVerification("attente2@example.com");

        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(eventPublisher).publishEvent(any(VerificationTokenIssuedEvent.class));
    }
}
