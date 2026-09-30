package ch.admin.zas.jweb.laforge.collective.domain;

import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

/** Inscription d'un compte à un défi ; le créateur est automatiquement inscrit à la création. */
@Entity
@Table(name = "challenge_participant",
        uniqueConstraints = @UniqueConstraint(name = "uk_challenge_participant", columnNames = {"challenge_id", "account_id"}))
public class ChallengeParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    protected ChallengeParticipant() {
        // Requis par JPA.
    }

    public ChallengeParticipant(Challenge challenge, Account account, OffsetDateTime joinedAt) {
        this.challenge = challenge;
        this.account = account;
        this.joinedAt = joinedAt;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public Account getAccount() {
        return account;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
