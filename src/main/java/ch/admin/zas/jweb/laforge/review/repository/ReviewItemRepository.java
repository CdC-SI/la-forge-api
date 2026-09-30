package ch.admin.zas.jweb.laforge.review.repository;

import ch.admin.zas.jweb.laforge.review.domain.ReviewItem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewItemRepository extends JpaRepository<ReviewItem, UUID>, JpaSpecificationExecutor<ReviewItem> {

    List<ReviewItem> findByLearner_IdOrderByDueAtAscIdAsc(UUID learnerId);

    Optional<ReviewItem> findByLearner_IdAndExerciseIdAndCompletedAtIsNull(UUID learnerId, UUID exerciseId);

    Optional<ReviewItem> findBySourceAttempt_Id(UUID attemptId);

    long countByLearner_IdAndCompletedAtIsNullAndDueAtLessThanEqual(UUID learnerId, OffsetDateTime now);
}
