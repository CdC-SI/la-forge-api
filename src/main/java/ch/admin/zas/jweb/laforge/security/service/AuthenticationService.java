package ch.admin.zas.jweb.laforge.security.service;

import ch.admin.zas.jweb.laforge.common.error.UnauthenticatedException;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vérifie les identifiants présentés à {@code POST /auth/sessions}. */
@Service
public class AuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Adresse e-mail ou mot de passe invalide.";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @throws UnauthenticatedException si l'adresse est inconnue ou le mot de passe incorrect
     * @throws ch.admin.zas.jweb.laforge.common.error.ForbiddenException si le compte ne peut pas
     *                                                                   encore s'authentifier (voir {@link Account#assertCanAuthenticate()})
     */
    @Transactional(readOnly = true)
    public Account authenticate(String email, String rawPassword) {
        var account = accountRepository.findByEmail(email.strip().toLowerCase())
                .orElseThrow(() -> new UnauthenticatedException(INVALID_CREDENTIALS_MESSAGE));
        if (!passwordEncoder.matches(rawPassword, account.getPasswordHash())) {
            throw new UnauthenticatedException(INVALID_CREDENTIALS_MESSAGE);
        }
        account.assertCanAuthenticate();
        return account;
    }
}
