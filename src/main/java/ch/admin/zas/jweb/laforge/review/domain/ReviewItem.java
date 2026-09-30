package ch.admin.zas.jweb.laforge.review.domain;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
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
import java.util.UUID;

/**
 * Fiche de révision espacée d'une tentative. Une seule fiche active (non {@code COMPLETED}) par
 * couple (apprenant, exercice) ; {@code exerciseId} est dénormalisé depuis {@link ExerciseVersion}
 * pour porter cette contrainte d'unicité sans recharger la version à chaque vérification.
 */
@Entity
@Table(name = "review_item")
public class ReviewItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Account learner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_version_id", nullable = false)
    private ExerciseVersion exerciseVersion;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_attempt_id", nullable = false)
    private Attempt sourceAttempt;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private ReviewReason reason;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected ReviewItem() {
        // Requis par JPA.
    }

    public ReviewItem(Account learner, ExerciseVersion exerciseVersion, Attempt sourceAttempt, OffsetDateTime dueAt,
            ReviewReason reason) {
        this.learner = learner;
        this.exerciseVersion = exerciseVersion;
        this.exerciseId = exerciseVersion.getExercise().getId();
        this.sourceAttempt = sourceAttempt;
        this.dueAt = dueAt;
        this.reason = reason;
    }

    /**
     * Replanifie l'échéance (autoévaluation ultérieure). Répéter la même autoévaluation ne décale
     * pas la date : à la charge de l'appelant de ne pas invoquer cette méthode dans ce cas.
     *
     * @throws InvalidStateException si la révision est déjà terminée
     */
    public void reschedule(OffsetDateTime newDueAt, ReviewReason newReason) {
        if (completedAt != null) {
            throw new InvalidStateException("Une révision terminée ne peut plus être replanifiée.");
        }
        this.dueAt = newDueAt;
        this.reason = newReason;
    }

    /** Clôture la révision, typiquement à la soumission de la tentative qui la traite. Idempotent. */
    public void complete(OffsetDateTime now) {
        if (completedAt == null) {
            completedAt = now;
        }
    }

    public ReviewState state(OffsetDateTime now) {
        if (completedAt != null) {
            return ReviewState.COMPLETED;
        }
        return dueAt.isAfter(now) ? ReviewState.SCHEDULED : ReviewState.DUE;
    }

    public Account getLearner() {
        return learner;
    }

    public ExerciseVersion getExerciseVersion() {
        return exerciseVersion;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public Attempt getSourceAttempt() {
        return sourceAttempt;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public ReviewReason getReason() {
        return reason;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
