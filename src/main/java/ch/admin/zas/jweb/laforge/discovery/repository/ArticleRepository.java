package ch.admin.zas.jweb.laforge.discovery.repository;

import ch.admin.zas.jweb.laforge.discovery.domain.Article;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArticleRepository extends JpaRepository<Article, UUID>, JpaSpecificationExecutor<Article> {
}
