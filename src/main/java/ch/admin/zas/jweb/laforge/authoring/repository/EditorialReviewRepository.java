package ch.admin.zas.jweb.laforge.authoring.repository;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.EditorialReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Accès aux relectures éditoriales, ordonnées par date de relecture. */
public interface EditorialReviewRepository extends JpaRepository<EditorialReview, UUID> {

    List<EditorialReview> findByDraftOrderByReviewedAtAsc(Draft draft);

    @Query("SELECT DISTINCT r.draft.id FROM EditorialReview r WHERE r.reviewer.id = :reviewerId")
    List<UUID> findDistinctDraftIdsByReviewer(@Param("reviewerId") UUID reviewerId);
}
