package ch.admin.zas.jweb.laforge.authoring.domain;

import ch.admin.zas.jweb.laforge.catalog.domain.CodeFile;
import ch.admin.zas.jweb.laforge.catalog.domain.CodeFilesConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.CorrectionConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.catalog.domain.HintsConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.LearningObjectivesConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpecConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirementsConverter;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.StaleVersionException;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Brouillon privé publié directement par son auteur ({@code DRAFT → PUBLISHED}). Le contenu réutilise les
 * mêmes types de valeur immuables que {@link ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion}
 * : à la publication, ce contenu est copié tel quel dans une nouvelle version figée.
 */
@Entity
@Table(name = "draft")
public class Draft extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Account author;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private DraftState state;

    @Version
    @Column(name = "revision", nullable = false)
    private int revision;

    @Column(name = "base_version", nullable = false)
    private int baseVersion;

    @Column(name = "published_version")
    private Integer publishedVersion;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private ExerciseType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20)
    private Difficulty difficulty;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "prompt_markdown", length = 20000)
    private String promptMarkdown;

    @Convert(converter = LearningObjectivesConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "learning_objectives", nullable = false, columnDefinition = "jsonb")
    private List<String> learningObjectives;

    @Convert(converter = CodeFilesConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "files", columnDefinition = "jsonb")
    private List<CodeFile> files;

    @Convert(converter = TechnologyRequirementsConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "technologies", columnDefinition = "jsonb")
    private List<TechnologyRequirement> technologies;

    @Convert(converter = ResponseSpecConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "response_spec", columnDefinition = "jsonb")
    private ResponseSpec responseSpec;

    @Convert(converter = HintsConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "hints", columnDefinition = "jsonb")
    private List<Hint> hints;

    @Convert(converter = CorrectionConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "correction", columnDefinition = "jsonb")
    private Correction correction;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "draft_topic",
            joinColumns = @JoinColumn(name = "draft_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<Topic> topics = new LinkedHashSet<>();

    protected Draft() {
        // Requis par JPA.
    }

    public Draft(
            Exercise exercise,
            Account author,
            int baseVersion,
            String title,
            ExerciseType type,
            Difficulty difficulty,
            Integer estimatedMinutes,
            String promptMarkdown,
            List<String> learningObjectives,
            List<CodeFile> files,
            List<TechnologyRequirement> technologies,
            ResponseSpec responseSpec,
            List<Hint> hints,
            Correction correction,
            Set<Topic> topics) {
        this.exercise = exercise;
        this.author = author;
        this.state = DraftState.DRAFT;
        this.baseVersion = baseVersion;
        replaceContent(0, title, type, difficulty, estimatedMinutes, promptMarkdown, learningObjectives, files, technologies,
                responseSpec, hints, correction, topics);
    }

    /**
     * Remplace intégralement le contenu éditorial.
     *
     * @throws StaleVersionException si {@code expectedRevision} ne correspond plus à la révision actuelle
     * @throws InvalidStateException si le brouillon n'est pas en état {@code DRAFT}
     */
    public final void replaceContent(
            int expectedRevision,
            String newTitle,
            ExerciseType newType,
            Difficulty newDifficulty,
            Integer newEstimatedMinutes,
            String newPromptMarkdown,
            List<String> newLearningObjectives,
            List<CodeFile> newFiles,
            List<TechnologyRequirement> newTechnologies,
            ResponseSpec newResponseSpec,
            List<Hint> newHints,
            Correction newCorrection,
            Set<Topic> newTopics) {
        requireEditable(expectedRevision);
        this.title = newTitle;
        this.type = newType;
        this.difficulty = newDifficulty;
        this.estimatedMinutes = newEstimatedMinutes;
        this.promptMarkdown = newPromptMarkdown;
        this.learningObjectives = newLearningObjectives == null ? List.of() : List.copyOf(newLearningObjectives);
        this.files = newFiles == null ? List.of() : List.copyOf(newFiles);
        this.technologies = newTechnologies == null ? List.of() : List.copyOf(newTechnologies);
        this.responseSpec = newResponseSpec;
        this.hints = newHints == null ? List.of() : List.copyOf(newHints);
        this.correction = newCorrection;
        this.topics = new LinkedHashSet<>(newTopics);
    }

    /**
     * Marque le brouillon comme publié sous le numéro de version indiqué.
     *
     * @throws StaleVersionException si {@code expectedRevision} ne correspond plus à la révision actuelle
     * @throws InvalidStateException si le brouillon n'est pas {@code DRAFT}
     */
    public void publish(int expectedRevision, int newPublishedVersion) {
        requireEditable(expectedRevision);
        state = DraftState.PUBLISHED;
        publishedVersion = newPublishedVersion;
    }

    public void requireEditable(int expectedRevision) {
        requireCurrentRevision(expectedRevision);
        requireState(DraftState.DRAFT, "Seul un brouillon en état DRAFT peut être modifié ou publié.");
    }

    private void requireCurrentRevision(int expectedRevision) {
        if (expectedRevision != revision) {
            throw new StaleVersionException("La révision fournie ne correspond plus à la révision actuelle du brouillon.");
        }
    }

    private void requireState(DraftState expected, String message) {
        if (state != expected) {
            throw new InvalidStateException(message);
        }
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Account getAuthor() {
        return author;
    }

    public DraftState getState() {
        return state;
    }

    public int getRevision() {
        return revision;
    }

    public int getBaseVersion() {
        return baseVersion;
    }

    public Integer getPublishedVersion() {
        return publishedVersion;
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

    public Integer getEstimatedMinutes() {
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

    public Set<Topic> getTopics() {
        return Set.copyOf(topics);
    }
}
