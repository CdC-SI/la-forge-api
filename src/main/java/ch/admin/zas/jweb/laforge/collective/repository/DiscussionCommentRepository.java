package ch.admin.zas.jweb.laforge.collective.repository;

import ch.admin.zas.jweb.laforge.collective.domain.DiscussionComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DiscussionCommentRepository extends JpaRepository<DiscussionComment, UUID>, JpaSpecificationExecutor<DiscussionComment> {

    List<DiscussionComment> findByChallenge_IdOrderByCreatedAtAscIdAsc(UUID challengeId);
}
