package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Thème du référentiel de pratique (ex. « Java », « Angular »). Lu via {@code GET /topics} et
 * enrichi via {@code POST /topics} (rôles AUTHOR ou ADMIN).
 */
@Entity
@Table(name = "topic", uniqueConstraints = @UniqueConstraint(name = "uk_topic_slug", columnNames = "slug"))
public class Topic extends BaseEntity {

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    protected Topic() {
        // Requis par JPA.
    }

    public Topic(String slug, String label) {
        this.slug = slug;
        this.label = label;
    }

    public String getSlug() {
        return slug;
    }

    public String getLabel() {
        return label;
    }
}
