package ch.admin.zas.jweb.laforge.security.domain;

import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

/**
 * Jeton d'activation de compte envoyé par courriel. Seul le hachage SHA-256 est persisté ; le
 * jeton en clair n'existe que dans le courriel envoyé à l'utilisateur. Usage unique, expire après
 * une durée fixe ({@code laforge.security.verification-token.ttl}).
 */
@Entity
@Table(name = "verification_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_verification_token_hash", columnNames = "token_hash"))
public class VerificationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    protected VerificationToken() {
        // Requis par JPA.
    }

    public VerificationToken(Account account, String tokenHash, OffsetDateTime expiresAt) {
        this.account = account;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(OffsetDateTime now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    public void consume(OffsetDateTime now) {
        if (!isUsable(now)) {
            throw new InvalidStateException("Le jeton de vérification est expiré ou déjà utilisé.");
        }
        consumedAt = now;
    }

    public Account getAccount() {
        return account;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }
}
