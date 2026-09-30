package ch.admin.zas.jweb.laforge.practice.domain;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Tentative de pratique d'une version d'exercice par un apprenant. {@code challengeId} et
 * {@code reviewItemId} sont des références faibles (non contraintes par clé étrangère) vers les
 * domaines {@code collective} et {@code review}, construits après {@code practice} ; leur
 * cohérence est garantie par les services applicatifs, pas par le schéma.
 */
@Entity
@Table(name = "attempt")
public class Attempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Account learner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_version_id", nullable = false)
    private ExerciseVersion exerciseVersion;

    @Column(name = "challenge_id")
    private UUID challengeId;

    @Column(name = "review_item_id")
    private UUID reviewItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Convert(converter = AnswerConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "answer", columnDefinition = "jsonb")
    private Answer answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "objective_result", length = 20)
    private ObjectiveResult objectiveResult;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attempt_revealed_hint", joinColumns = @JoinColumn(name = "attempt_id"))
    @Column(name = "hint_level", nullable = false)
    private SortedSet<Integer> revealedHintLevels = new TreeSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "self_assessment_mastery", length = 20)
    private SelfAssessmentMastery selfAssessmentMastery;

    @Column(name = "self_assessment_note", length = 2000)
    private String selfAssessmentNote;

    protected Attempt() {
        // Requis par JPA.
    }

    public Attempt(Account learner, ExerciseVersion exerciseVersion, UUID challengeId, UUID reviewItemId, OffsetDateTime now) {
        this.learner = learner;
        this.exerciseVersion = exerciseVersion;
        this.challengeId = challengeId;
        this.reviewItemId = reviewItemId;
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = now;
    }

    /**
     * Fige la réponse définitive et le résultat de correction automatique.
     *
     * @throws InvalidStateException si la tentative n'est plus {@code IN_PROGRESS}
     */
    public void submit(Answer submittedAnswer, ObjectiveResult result, OffsetDateTime now) {
        if (status != AttemptStatus.IN_PROGRESS) {
            throw new InvalidStateException("La tentative n'est plus en cours et ne peut plus être soumise.");
        }
        this.answer = submittedAnswer;
        this.objectiveResult = result;
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = now;
    }

    /**
     * Abandonne une tentative individuelle. Idempotent si déjà abandonnée.
     *
     * @throws InvalidStateException si la tentative est déjà soumise ou liée à un défi
     */
    public void abandon() {
        if (status == AttemptStatus.ABANDONED) {
            return;
        }
        if (status == AttemptStatus.SUBMITTED || challengeId != null) {
            throw new InvalidStateException("Une tentative soumise ou liée à un défi ne peut pas être abandonnée.");
        }
        status = AttemptStatus.ABANDONED;
    }

    /**
     * Débloque un indice ; les niveaux se révèlent strictement dans l'ordre à partir de 1.
     * Idempotent si le niveau est déjà révélé.
     *
     * @throws InvalidStateException si la tentative n'est pas en cours ou si le niveau demandé saute un niveau non révélé
     * @throws ForbiddenException    si la tentative est liée à un défi collectif
     */
    public void revealHint(int level) {
        if (status != AttemptStatus.IN_PROGRESS) {
            throw new InvalidStateException("Seule une tentative en cours permet de débloquer un indice.");
        }
        if (challengeId != null) {
            throw new ForbiddenException("Les indices sont interdits pendant un défi collectif.");
        }
        if (revealedHintLevels.contains(level)) {
            return;
        }
        int nextExpectedLevel = revealedHintLevels.isEmpty() ? 1 : revealedHintLevels.last() + 1;
        if (level != nextExpectedLevel) {
            throw new InvalidStateException("Les indices doivent être débloqués dans l'ordre, à partir du niveau 1.");
        }
        revealedHintLevels.add(level);
    }

    /**
     * Enregistre ou remplace l'autoévaluation de maîtrise. Ne décale pas la planification si la
     * même valeur est répétée (calcul du délai laissé au service appelant).
     *
     * @throws InvalidStateException si la tentative n'est pas encore soumise
     */
    public void recordSelfAssessment(SelfAssessmentMastery mastery, String note) {
        if (status != AttemptStatus.SUBMITTED) {
            throw new InvalidStateException("L'autoévaluation exige une tentative déjà soumise.");
        }
        this.selfAssessmentMastery = mastery;
        this.selfAssessmentNote = note;
    }

    public boolean isDebriefAvailable() {
        return status == AttemptStatus.SUBMITTED;
    }

    public Account getLearner() {
        return learner;
    }

    public ExerciseVersion getExerciseVersion() {
        return exerciseVersion;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public UUID getReviewItemId() {
        return reviewItemId;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Answer getAnswer() {
        return answer;
    }

    public ObjectiveResult getObjectiveResult() {
        return objectiveResult;
    }

    public SortedSet<Integer> getRevealedHintLevels() {
        return java.util.Collections.unmodifiableSortedSet(revealedHintLevels);
    }

    public SelfAssessmentMastery getSelfAssessmentMastery() {
        return selfAssessmentMastery;
    }

    public String getSelfAssessmentNote() {
        return selfAssessmentNote;
    }
}
