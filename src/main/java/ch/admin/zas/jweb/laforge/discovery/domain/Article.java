package ch.admin.zas.jweb.laforge.discovery.domain;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirementsConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.common.error.StaleVersionException;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Fiche de veille technique, publiée directement par un {@code AUTHOR}/{@code ADMIN} (pas de
 * cycle brouillon). {@code revision} porte le verrouillage optimiste exigé par le contrat
 * ({@code expectedRevision} sur {@code PUT /authoring/articles/{id}}).
 */
@Entity
@Table(name = "article")
public class Article extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary", nullable = false, length = 2000)
    private String summary;

    @Column(name = "body_markdown", nullable = false, length = 20000)
    private String bodyMarkdown;

    @Convert(converter = TechnologyRequirementsConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "technologies", columnDefinition = "jsonb")
    private List<TechnologyRequirement> technologies;

    @Convert(converter = ArticleSourcesConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "sources", nullable = false, columnDefinition = "jsonb")
    private List<Source> sources;

    @Column(name = "published_at", nullable = false, updatable = false)
    private OffsetDateTime publishedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "article_topic",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<Topic> topics = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "article_related_exercise_version",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "exercise_version_id"))
    private Set<ExerciseVersion> relatedExerciseVersions = new LinkedHashSet<>();

    @Version
    @Column(name = "revision", nullable = false)
    private int revision;

    protected Article() {
        // Requis par JPA.
    }

    public Article(
            String title,
            String summary,
            String bodyMarkdown,
            List<TechnologyRequirement> technologies,
            List<Source> sources,
            Set<Topic> topics,
            Set<ExerciseVersion> relatedExerciseVersions,
            OffsetDateTime now) {
        this.title = title;
        this.summary = summary;
        this.bodyMarkdown = bodyMarkdown;
        this.technologies = technologies == null ? List.of() : List.copyOf(technologies);
        this.sources = List.copyOf(sources);
        this.topics = new LinkedHashSet<>(topics);
        this.relatedExerciseVersions = new LinkedHashSet<>(relatedExerciseVersions);
        this.publishedAt = now;
    }

    /**
     * Corrige le contenu de la fiche. {@code publishedAt} n'est jamais modifié ; seule
     * {@code updatedAt} (hérité de {@link BaseEntity}) évolue, aux côtés de {@code revision}.
     *
     * @throws StaleVersionException si {@code expectedRevision} ne correspond plus à la révision courante
     */
    public void update(
            int expectedRevision,
            String newTitle,
            String newSummary,
            String newBodyMarkdown,
            List<TechnologyRequirement> newTechnologies,
            List<Source> newSources,
            Set<Topic> newTopics,
            Set<ExerciseVersion> newRelatedExerciseVersions) {
        if (expectedRevision != revision) {
            throw new StaleVersionException("La révision fournie ne correspond plus à la révision actuelle de la fiche.");
        }
        this.title = newTitle;
        this.summary = newSummary;
        this.bodyMarkdown = newBodyMarkdown;
        this.technologies = newTechnologies == null ? List.of() : List.copyOf(newTechnologies);
        this.sources = List.copyOf(newSources);
        this.topics = new LinkedHashSet<>(newTopics);
        this.relatedExerciseVersions = new LinkedHashSet<>(newRelatedExerciseVersions);
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public List<TechnologyRequirement> getTechnologies() {
        return technologies;
    }

    public List<Source> getSources() {
        return sources;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public Set<Topic> getTopics() {
        return Set.copyOf(topics);
    }

    public Set<ExerciseVersion> getRelatedExerciseVersions() {
        return Set.copyOf(relatedExerciseVersions);
    }

    public int getRevision() {
        return revision;
    }
}
