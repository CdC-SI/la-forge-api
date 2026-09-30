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
import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.StaleVersionException;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.security.domain.Account;
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
import jakarta.persistence.Version;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Brouillon d'exercice porté par la machine à états éditoriale {@code DRAFT → IN_REVIEW →
 * APPROVED → PUBLISHED} (retour {@code DRAFT} si changements demandés). Le contenu réutilise les
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
    @Column(name = "response_spec", nullable = false, columnDefinition = "jsonb")
    private ResponseSpec responseSpec;

    @Convert(converter = HintsConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "hints", columnDefinition = "jsonb")
    private List<Hint> hints;

    @Convert(converter = CorrectionConverter.class)
    @ColumnTransformer(write = "cast(? as jsonb)")
    @Column(name = "correction", nullable = false, columnDefinition = "jsonb")
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
            int newEstimatedMinutes,
            String newPromptMarkdown,
            List<String> newLearningObjectives,
            List<CodeFile> newFiles,
            List<TechnologyRequirement> newTechnologies,
            ResponseSpec newResponseSpec,
            List<Hint> newHints,
            Correction newCorrection,
            Set<Topic> newTopics) {
        requireCurrentRevision(expectedRevision);
        if (state != null && state != DraftState.DRAFT) {
            throw new InvalidStateException("Seul un brouillon en état DRAFT peut être remplacé.");
        }
        this.title = newTitle;
        this.type = newType;
        this.difficulty = newDifficulty;
        this.estimatedMinutes = newEstimatedMinutes;
        this.promptMarkdown = newPromptMarkdown;
        this.learningObjectives = List.copyOf(newLearningObjectives);
        this.files = newFiles == null ? List.of() : List.copyOf(newFiles);
        this.technologies = newTechnologies == null ? List.of() : List.copyOf(newTechnologies);
        this.responseSpec = newResponseSpec;
        this.hints = newHints == null ? List.of() : List.copyOf(newHints);
        this.correction = newCorrection;
        this.topics = new LinkedHashSet<>(newTopics);
    }

    /**
     * @throws StaleVersionException si {@code expectedRevision} ne correspond plus à la révision actuelle
     * @throws InvalidStateException si le brouillon n'est pas en état {@code DRAFT}
     */
    public void submitForReview(int expectedRevision) {
        requireCurrentRevision(expectedRevision);
        requireState(DraftState.DRAFT, "Seul un brouillon en état DRAFT peut être soumis à relecture.");
        state = DraftState.IN_REVIEW;
    }

    /**
     * @throws StaleVersionException si {@code expectedRevision} ne correspond plus à la révision actuelle
     * @throws InvalidStateException si le brouillon n'est pas en état {@code IN_REVIEW}
     * @throws ForbiddenException    si le relecteur est l'auteur du contenu (auto-approbation interdite hors ADMIN)
     */
    public void approve(int expectedRevision, Account reviewer, boolean reviewerIsAdmin) {
        requireCurrentRevision(expectedRevision);
        requireState(DraftState.IN_REVIEW, "Seul un brouillon en relecture peut être approuvé.");
        if (!reviewerIsAdmin && reviewer.getId().equals(author.getId())) {
            throw new ForbiddenException("Un relecteur ne peut pas approuver son propre contenu.");
        }
        state = DraftState.APPROVED;
    }

    /**
     * @throws StaleVersionException si {@code expectedRevision} ne correspond plus à la révision actuelle
     * @throws InvalidStateException si le brouillon n'est pas en état {@code IN_REVIEW}
     */
    public void requestChanges(int expectedRevision) {
        requireCurrentRevision(expectedRevision);
        requireState(DraftState.IN_REVIEW, "Seul un brouillon en relecture peut recevoir une demande de changements.");
        state = DraftState.DRAFT;
    }

    /**
     * Marque le brouillon comme publié sous le numéro de version indiqué.
     *
     * @throws StaleVersionException si {@code expectedRevision} ne correspond plus à la révision actuelle
     * @throws InvalidStateException si le brouillon n'est pas {@code APPROVED}
     */
    public void publish(int expectedRevision, int newPublishedVersion) {
        requireCurrentRevision(expectedRevision);
        requireState(DraftState.APPROVED, "Seul un brouillon approuvé peut être publié.");
        state = DraftState.PUBLISHED;
        publishedVersion = newPublishedVersion;
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

    public Set<Topic> getTopics() {
        return Set.copyOf(topics);
    }
}
