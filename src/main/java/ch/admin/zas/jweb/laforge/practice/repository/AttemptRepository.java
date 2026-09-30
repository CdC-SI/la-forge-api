package ch.admin.zas.jweb.laforge.practice.repository;

import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttemptRepository extends JpaRepository<Attempt, UUID>, JpaSpecificationExecutor<Attempt> {

    List<Attempt> findByLearner_IdOrderByStartedAtDescIdDesc(UUID learnerId);

    Optional<Attempt> findByIdAndLearner_Id(UUID id, UUID learnerId);

    boolean existsByLearner_IdAndExerciseVersion_IdAndStatus(UUID learnerId, UUID exerciseVersionId, AttemptStatus status);

    Optional<Attempt> findByReviewItemId(UUID reviewItemId);

    List<Attempt> findByChallengeIdAndStatusOrderBySubmittedAtAscIdAsc(UUID challengeId, AttemptStatus status);

    Optional<Attempt> findByChallengeIdAndLearner_Id(UUID challengeId, UUID learnerId);

    long countByLearner_IdAndStatus(UUID learnerId, AttemptStatus status);

    /**
     * Vrai si l'apprenant a déjà révélé un indice sur cette version d'exercice en dehors du défi
     * fourni (y compris les tentatives hors défi), ce qui interdit toute nouvelle tentative de défi.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM Attempt a JOIN a.revealedHintLevels h
            WHERE a.learner.id = :learnerId AND a.exerciseVersion.id = :versionId
            AND (a.challengeId IS NULL OR a.challengeId <> :challengeId)
            """)
    boolean existsRevealedHintOutsideChallenge(
            @Param("learnerId") UUID learnerId, @Param("versionId") UUID versionId, @Param("challengeId") UUID challengeId);

    @Query("SELECT a.submittedAt FROM Attempt a WHERE a.learner.id = :learnerId AND a.status = ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus.SUBMITTED"
            + " AND a.submittedAt >= :since")
    List<OffsetDateTime> findSubmittedAtSince(@Param("learnerId") UUID learnerId, @Param("since") OffsetDateTime since);

    @Query("""
            SELECT t.id AS topicId,
                   COUNT(a) AS submittedAttempts,
                   SUM(CASE WHEN a.objectiveResult = ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult.CORRECT THEN 1L ELSE 0L END) AS correctChoiceAttempts,
                   SUM(CASE WHEN a.objectiveResult <> ch.admin.zas.jweb.laforge.practice.domain.ObjectiveResult.NOT_AUTO_GRADED THEN 1L ELSE 0L END) AS gradedChoiceAttempts,
                   MAX(a.submittedAt) AS lastPracticedAt
            FROM Attempt a JOIN a.exerciseVersion.topics t
            WHERE a.learner.id = :learnerId AND a.status = ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus.SUBMITTED
            GROUP BY t.id
            """)
    List<TopicProgressProjection> findTopicProgress(@Param("learnerId") UUID learnerId);
}
