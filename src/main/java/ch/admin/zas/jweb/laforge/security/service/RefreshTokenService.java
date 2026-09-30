package ch.admin.zas.jweb.laforge.security.service;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import ch.admin.zas.jweb.laforge.common.error.UnauthenticatedException;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.RefreshToken;
import ch.admin.zas.jweb.laforge.security.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Émission et rotation des jetons de renouvellement opaques. Chaque connexion initiale crée une
 * famille de jetons ({@code familyId}) ; chaque renouvellement révoque le jeton consommé et en
 * émet un nouveau dans la même famille. La présentation d'un jeton déjà révoqué révoque toute la
 * famille : signe probable qu'un jeton a fuité et a été rejoué par un tiers.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final SecureTokenFactory tokenFactory;
    private final LaForgeProperties properties;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            SecureTokenFactory tokenFactory,
            LaForgeProperties properties,
            Clock clock) {
        this.repository = repository;
        this.tokenFactory = tokenFactory;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedRefreshToken issueNewFamily(Account account) {
        return issue(account, UUID.randomUUID());
    }

    /**
     * Valide et consomme le jeton présenté, puis émet son remplaçant dans la même famille.
     *
     * @throws UnauthenticatedException si le jeton est inconnu, expiré ou déjà consommé (auquel
     *                                  cas toute la famille est révoquée par précaution)
     */
    @Transactional
    public IssuedRefreshToken rotate(String rawToken) {
        var hash = tokenFactory.hash(rawToken);
        var current = repository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthenticatedException("Jeton de renouvellement invalide."));
        var now = OffsetDateTime.now(clock);
        if (!current.isActive(now)) {
            repository.findByFamilyId(current.getFamilyId()).forEach(token -> token.revoke(now));
            throw new UnauthenticatedException("Jeton de renouvellement invalide, expiré ou déjà utilisé.");
        }
        current.revoke(now);
        return issue(current.getAccount(), current.getFamilyId());
    }

    @Transactional
    public void revoke(String rawToken) {
        var hash = tokenFactory.hash(rawToken);
        repository.findByTokenHash(hash).ifPresent(token -> token.revoke(OffsetDateTime.now(clock)));
    }

    private IssuedRefreshToken issue(Account account, UUID familyId) {
        var rawToken = tokenFactory.generateOpaqueToken();
        var expiresAt = OffsetDateTime.now(clock).plus(properties.security().refreshToken().ttl());
        var entity = new RefreshToken(account, tokenFactory.hash(rawToken), familyId, expiresAt);
        repository.save(entity);
        return new IssuedRefreshToken(rawToken, entity.getAccount());
    }

    public record IssuedRefreshToken(String rawToken, Account account) {
    }
}
