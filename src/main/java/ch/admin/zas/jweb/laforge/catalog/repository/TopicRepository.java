package ch.admin.zas.jweb.laforge.catalog.repository;

import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TopicRepository extends JpaRepository<Topic, UUID>, JpaSpecificationExecutor<Topic> {

    boolean existsBySlug(String slug);
}
