package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Version numérotée et immuable d'un exercice. Une fois {@code publishedAt} renseigné, aucune
 * opération d'écriture n'est exposée : toute évolution passe par une nouvelle version créée par
 * le domaine {@code authoring}.
 */
@Entity
@Table(name = "exercise_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_exercise_version_number", columnNames = {"exercise_id", "version_number"}))
public class ExerciseVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ExerciseType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(name = "prompt_markdown", nullable = false, length = 20000)
    private String promptMarkdown;

    @Convert(converter = LearningObjectivesConverter.class)
    @Column(name = "learning_objectives", nullable = false, columnDefinition = "jsonb")
    private List<String> learningObjectives;

    @Convert(converter = CodeFilesConverter.class)
    @Column(name = "files", columnDefinition = "jsonb")
    private List<CodeFile> files;

    @Convert(converter = TechnologyRequirementsConverter.class)
    @Column(name = "technologies", columnDefinition = "jsonb")
    private List<TechnologyRequirement> technologies;

    @Convert(converter = ResponseSpecConverter.class)
    @Column(name = "response_spec", nullable = false, columnDefinition = "jsonb")
    private ResponseSpec responseSpec;

    @Convert(converter = HintsConverter.class)
    @Column(name = "hints", columnDefinition = "jsonb")
    private List<Hint> hints;

    @Convert(converter = CorrectionConverter.class)
    @Column(name = "correction", nullable = false, columnDefinition = "jsonb")
    private Correction correction;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "exercise_version_topic",
            joinColumns = @JoinColumn(name = "exercise_version_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<Topic> topics = new LinkedHashSet<>();

    protected ExerciseVersion() {
        // Requis par JPA.
    }

    public ExerciseVersion(
            Exercise exercise,
            int versionNumber,
            String title,
            ExerciseType type,
            Difficulty difficulty,
            int estimatedMinutes,
            String promptMarkdown,
            List<String> learningObjectives,
            List<CodeFile> files,
            List<TechnologyRequirement> technologies,
            ResponseSpec responseSpec,
            List<Hint> hints,
            Correction correction,
            Set<Topic> topics) {
        this.exercise = exercise;
        this.versionNumber = versionNumber;
        this.title = title;
        this.type = type;
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.promptMarkdown = promptMarkdown;
        this.learningObjectives = List.copyOf(learningObjectives);
        this.files = files == null ? List.of() : List.copyOf(files);
        this.technologies = technologies == null ? List.of() : List.copyOf(technologies);
        this.responseSpec = responseSpec;
        this.hints = hints == null ? List.of() : List.copyOf(hints);
        this.correction = correction;
        this.topics = new LinkedHashSet<>(topics);
    }

    /** Marque cette version comme publiée à l'instant fourni ; opération unique et définitive. */
    public void publish(OffsetDateTime now) {
        if (publishedAt == null) {
            publishedAt = now;
        }
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public ExerciseType getType() {
        return type;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public String getPromptMarkdown() {
        return promptMarkdown;
    }

    public List<String> getLearningObjectives() {
        return learningObjectives;
    }

    public List<CodeFile> getFiles() {
        return files;
    }

    public List<TechnologyRequirement> getTechnologies() {
        return technologies;
    }

    public ResponseSpec getResponseSpec() {
        return responseSpec;
    }

    public List<Hint> getHints() {
        return hints;
    }

    public Correction getCorrection() {
        return correction;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public Set<Topic> getTopics() {
        return Set.copyOf(topics);
    }
}
