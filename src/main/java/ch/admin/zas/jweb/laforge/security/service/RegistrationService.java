package ch.admin.zas.jweb.laforge.security.service;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.VerificationToken;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.security.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Création de compte et renvoi du courriel de confirmation. Ne révèle jamais si une adresse est
 * déjà enregistrée : les deux opérations se terminent silencieusement (202) dans ce cas, l'envoi
 * effectif du courriel étant la seule différence observable (et uniquement par le titulaire de la
 * boîte de réception).
 */
@Service
public class RegistrationService {

    private final AccountRepository accountRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenFactory tokenFactory;
    private final LaForgeProperties properties;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public RegistrationService(
            AccountRepository accountRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordEncoder passwordEncoder,
            SecureTokenFactory tokenFactory,
            LaForgeProperties properties,
            Clock clock,
            ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenFactory = tokenFactory;
        this.properties = properties;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(String email, String rawPassword, String displayName) {
        var normalizedEmail = normalize(email);
        if (accountRepository.existsByEmail(normalizedEmail)) {
            return;
        }
        var account = new Account(normalizedEmail, passwordEncoder.encode(rawPassword), displayName);
        accountRepository.save(account);
        issueVerificationToken(account);
    }

    @Transactional
    public void resendVerification(String email) {
        var normalizedEmail = normalize(email);
        accountRepository.findByEmail(normalizedEmail)
                .filter(account -> account.getStatus() == AccountStatus.PENDING_VERIFICATION)
                .filter(this::isOutsideCooldown)
                .ifPresent(this::issueVerificationToken);
    }

    private boolean isOutsideCooldown(Account account) {
        var now = OffsetDateTime.now(clock);
        var cooldownStart = now.minus(properties.security().verificationToken().resendCooldown());
        return verificationTokenRepository.findByAccountAndConsumedAtIsNullAndExpiresAtAfter(account, now).stream()
                .noneMatch(token -> token.getCreatedAt().isAfter(cooldownStart));
    }

    private void issueVerificationToken(Account account) {
        var rawToken = tokenFactory.generateOpaqueToken();
        var expiresAt = OffsetDateTime.now(clock).plus(properties.security().verificationToken().ttl());
        var token = new VerificationToken(account, tokenFactory.hash(rawToken), expiresAt);
        verificationTokenRepository.save(token);
        eventPublisher.publishEvent(
                new VerificationTokenIssuedEvent(account.getId(), account.getEmail(), account.getDisplayName(), rawToken));
    }

    private String normalize(String email) {
        return email.strip().toLowerCase();
    }
}
