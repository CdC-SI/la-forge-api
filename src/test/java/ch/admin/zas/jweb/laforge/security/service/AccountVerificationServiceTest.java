package ch.admin.zas.jweb.laforge.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.domain.VerificationToken;
import ch.admin.zas.jweb.laforge.security.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests unitaires purs (Mockito) de {@link AccountVerificationService}. */
@ExtendWith(MockitoExtension.class)
class AccountVerificationServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private SecureTokenFactory tokenFactory;

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);
    private AccountVerificationService service;

    @BeforeEach
    void setUp() {
        service = new AccountVerificationService(verificationTokenRepository, tokenFactory, clock);
    }

    private static Account newPendingAccount() {
        var account = new Account("learner@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    @Test
    void verify_jetonInconnu_declencheInvalidStateException() {
        when(tokenFactory.hash("raw")).thenReturn("hashed");
        when(verificationTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.empty());

        assertThrows(InvalidStateException.class, () -> service.verify("raw"));
    }

    @Test
    void verify_jetonExpire_declencheInvalidStateException() {
        var account = newPendingAccount();
        var expiredToken = new VerificationToken(account, "hashed", OffsetDateTime.now(clock).minusMinutes(1));
        when(tokenFactory.hash("raw")).thenReturn("hashed");
        when(verificationTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidStateException.class, () -> service.verify("raw"));
        assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    void verify_jetonDejaConsomme_declencheInvalidStateException() {
        var account = newPendingAccount();
        var token = new VerificationToken(account, "hashed", OffsetDateTime.now(clock).plusHours(1));
        token.consume(OffsetDateTime.now(clock).minusMinutes(1));
        when(tokenFactory.hash("raw")).thenReturn("hashed");
        when(verificationTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        assertThrows(InvalidStateException.class, () -> service.verify("raw"));
    }

    @Test
    void verify_jetonValide_activeLeCompteEtAccordeLeRoleLearner() {
        var account = newPendingAccount();
        var token = new VerificationToken(account, "hashed", OffsetDateTime.now(clock).plusHours(1));
        when(tokenFactory.hash("raw")).thenReturn("hashed");
        when(verificationTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        var activated = service.verify("raw");

        assertThat(activated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(activated.getRoles()).contains(Role.LEARNER);
        assertThat(token.getConsumedAt()).isEqualTo(OffsetDateTime.now(clock));
    }
}
