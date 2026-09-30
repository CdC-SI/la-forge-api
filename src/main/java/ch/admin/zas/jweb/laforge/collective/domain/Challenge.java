package ch.admin.zas.jweb.laforge.collective.domain;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Défi collectif portant sur une version publiée précise d'exercice. Le code d'invitation n'est
 * jamais stocké en clair : seule son empreinte SHA-256 ({@link
 * ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory}) est persistée, à l'image des
 * jetons de sécurité.
 */
@Entity
@Table(name = "challenge")
public class Challenge extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_version_id", nullable = false)
    private ExerciseVersion exerciseVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Account creator;

    @Column(name = "closes_at", nullable = false)
    private OffsetDateTime closesAt;

    @Column(name = "join_code_hash", nullable = false, length = 64)
    private String joinCodeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private ChallengeState state;

    protected Challenge() {
        // Requis par JPA.
    }

    public Challenge(String title, ExerciseVersion exerciseVersion, Account creator, OffsetDateTime closesAt, String joinCodeHash) {
        this.title = title;
        this.exerciseVersion = exerciseVersion;
        this.creator = creator;
        this.closesAt = closesAt;
        this.joinCodeHash = joinCodeHash;
        this.state = ChallengeState.OPEN;
    }

    /** Ferme le défi, révoquant le code d'invitation. Idempotent si déjà fermé. */
    public void close() {
        state = ChallengeState.CLOSED;
    }

    public boolean isOpen() {
        return state == ChallengeState.OPEN;
    }

    public String getTitle() {
        return title;
    }

    public ExerciseVersion getExerciseVersion() {
        return exerciseVersion;
    }

    public Account getCreator() {
        return creator;
    }

    public OffsetDateTime getClosesAt() {
        return closesAt;
    }

    public String getJoinCodeHash() {
        return joinCodeHash;
    }

    public ChallengeState getState() {
        return state;
    }
}
