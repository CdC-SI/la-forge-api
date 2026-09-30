package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Thème du référentiel de pratique (ex. « Java », « Angular »). Référentiel géré hors API en v1 ;
 * seule sa lecture est exposée ({@code GET /topics}).
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
