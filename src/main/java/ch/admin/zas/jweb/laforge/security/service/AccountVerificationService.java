package ch.admin.zas.jweb.laforge.security.service;

import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirmation de compte à partir du jeton reçu par courriel. */
@Service
public class AccountVerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final SecureTokenFactory tokenFactory;
    private final Clock clock;

    public AccountVerificationService(
            VerificationTokenRepository verificationTokenRepository, SecureTokenFactory tokenFactory, Clock clock) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.tokenFactory = tokenFactory;
        this.clock = clock;
    }

    /**
     * @throws InvalidStateException si le jeton est inconnu, expiré ou déjà utilisé
     */
    @Transactional
    public Account verify(String rawToken) {
        var token = verificationTokenRepository.findByTokenHash(tokenFactory.hash(rawToken))
                .orElseThrow(() -> new InvalidStateException("Jeton de vérification invalide, expiré ou déjà utilisé."));
        token.consume(OffsetDateTime.now(clock));
        var account = token.getAccount();
        account.activate();
        return account;
    }
}
