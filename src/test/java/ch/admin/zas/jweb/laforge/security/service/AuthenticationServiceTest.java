package ch.admin.zas.jweb.laforge.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.UnauthenticatedException;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests unitaires purs (Mockito) de {@link AuthenticationService}. */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(accountRepository, passwordEncoder);
    }

    private static Account newActiveAccount() {
        var account = new Account("learner@example.com", "hashed", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.activate();
        return account;
    }

    @Test
    void authenticate_adresseInconnue_declencheUnauthenticatedException() {
        when(accountRepository.findByEmail("absent@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthenticatedException.class, () -> service.authenticate("absent@example.com", "secret"));
    }

    @Test
    void authenticate_motDePasseIncorrect_declencheUnauthenticatedException() {
        var account = newActiveAccount();
        when(accountRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("mauvais", "hashed")).thenReturn(false);

        assertThrows(UnauthenticatedException.class, () -> service.authenticate("learner@example.com", "mauvais"));
    }

    @Test
    void authenticate_compteNonActif_declencheForbiddenException() {
        var account = new Account("pending@example.com", "hashed", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        when(accountRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> service.authenticate("pending@example.com", "secret"));
    }

    @Test
    void authenticate_identifiantsValides_retourneLeCompte() {
        var account = newActiveAccount();
        when(accountRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        var authenticated = service.authenticate(" Learner@Example.com ", "secret");

        assertThat(authenticated).isEqualTo(account);
    }
}
