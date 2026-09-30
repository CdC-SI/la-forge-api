package ch.admin.zas.jweb.laforge.authoring.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.catalog.domain.CodeFileKind;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.error.StaleVersionException;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link DraftRepository} : round-trip des sous-documents {@code jsonb}
 * partagés avec {@code ExerciseVersion}, verrouillage optimiste ({@code revision}) et requêtes
 * dérivées utilisées par la machine à états éditoriale.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class DraftRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DraftRepository draftRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Author"));
    }

    private Exercise persistExercise() {
        return entityManager.persistAndFlush(new Exercise());
    }

    private Draft sampleDraft(Exercise exercise, Account author, Set<Topic> topics) {
        return new Draft(
                exercise,
                author,
                0,
                "Titre du brouillon",
                ExerciseType.CODE_REVIEW,
                Difficulty.INTERMEDIATE,
                20,
                "Énoncé",
                List.of("Objectif 1"),
                ExerciseVersionFixtures.sampleFiles(),
                ExerciseVersionFixtures.sampleTechnologies(),
                ExerciseVersionFixtures.sampleResponseSpec(),
                ExerciseVersionFixtures.sampleHints(),
                ExerciseVersionFixtures.sampleCorrection(),
                topics);
    }

    @Test
    void saveThenFindById_roundTripsJsonbFieldsAndTopics() {
        var exercise = persistExercise();
        var author = persistAccount("author@example.com");
        var topic = entityManager.persistAndFlush(new Topic("java", "Java"));
        var draft = sampleDraft(exercise, author, new LinkedHashSet<>(Set.of(topic)));

        var saved = draftRepository.saveAndFlush(draft);
        entityManager.clear();

        var reloaded = draftRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getExercise().getId()).isEqualTo(exercise.getId());
        assertThat(reloaded.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(reloaded.getState()).isEqualTo(DraftState.DRAFT);
        assertThat(reloaded.getBaseVersion()).isZero();
        assertThat(reloaded.getPublishedVersion()).isNull();
        assertThat(reloaded.getLearningObjectives()).containsExactly("Objectif 1");
        assertThat(reloaded.getFiles().get(0).kind()).isEqualTo(CodeFileKind.SOURCE);
        assertThat(reloaded.getCorrection().correctChoiceIds()).containsExactly("a");
        assertThat(reloaded.getTopics()).extracting("id").containsExactly(topic.getId());
        assertThat(reloaded.getRevision()).isZero();
    }

    @Test
    void editorialWorkflow_incrementsRevisionAndRejectsStaleRevision() {
        var exercise = persistExercise();
        var author = persistAccount("workflow@example.com");
        var draft = draftRepository.saveAndFlush(sampleDraft(exercise, author, Set.of()));
        entityManager.clear();

        var managed = draftRepository.findById(draft.getId()).orElseThrow();
        managed.publish(0, 1);
        draftRepository.saveAndFlush(managed);
        entityManager.clear();

        var reloaded = draftRepository.findById(draft.getId()).orElseThrow();
        assertThat(reloaded.getState()).isEqualTo(DraftState.PUBLISHED);
        assertThat(reloaded.getRevision()).isEqualTo(1);

        org.junit.jupiter.api.Assertions.assertThrows(StaleVersionException.class, () -> reloaded.publish(0, 1));
    }

    @Test
    void titleOnlyDraft_roundTripsAndKeepsOwnershipPrivate() {
        var exercise = persistExercise();
        var author = persistAccount("minimal@example.com");
        var other = persistAccount("outsider@example.com");
        var draft = draftRepository.saveAndFlush(new Draft(
                exercise, author, 0, "Titre", null, null, null, null, null,
                null, List.of(new TechnologyRequirement("Java", null, null, null)),
                null, null, null, Set.of()));
        entityManager.clear();

        var reloaded = draftRepository.findByIdAndAuthor_Id(draft.getId(), author.getId()).orElseThrow();
        assertThat(reloaded.getType()).isNull();
        assertThat(reloaded.getDifficulty()).isNull();
        assertThat(reloaded.getEstimatedMinutes()).isNull();
        assertThat(reloaded.getPromptMarkdown()).isNull();
        assertThat(reloaded.getResponseSpec()).isNull();
        assertThat(reloaded.getCorrection()).isNull();
        assertThat(reloaded.getLearningObjectives()).isEmpty();
        assertThat(reloaded.getTechnologies()).containsExactly(new TechnologyRequirement("Java", null, null, null));
        assertThat(draftRepository.findByIdAndAuthor_Id(draft.getId(), other.getId())).isEmpty();
    }

    @Test
    void replacingContentFlushesTheNewRevisionAndCanClearOptionalFields() {
        var draft = draftRepository.saveAndFlush(sampleDraft(
                persistExercise(), persistAccount("replace@example.com"), Set.of()));
        var previousRevision = draft.getRevision();
        draft.replaceContent(previousRevision, "Nouveau titre", null, null, null,
                null, null, null, null, null, null, null, Set.of());
        draftRepository.flush();

        assertThat(draft.getRevision()).isEqualTo(previousRevision + 1);
        entityManager.clear();
        var reloaded = draftRepository.findById(draft.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Nouveau titre");
        assertThat(reloaded.getCorrection()).isNull();
        assertThat(reloaded.getTechnologies()).isEmpty();
        assertThat(reloaded.getLearningObjectives()).isEmpty();
    }

    @Test
    void findByExerciseIdAndStateNot_locatesTheSingleActiveDraft() {
        var exercise = persistExercise();
        var author = persistAccount("active@example.com");
        var draft = draftRepository.saveAndFlush(sampleDraft(exercise, author, Set.of()));
        entityManager.clear();

        var active = draftRepository.findByExerciseIdAndStateNot(exercise.getId(), DraftState.PUBLISHED);

        assertThat(active).isPresent();
        assertThat(active.orElseThrow().getId()).isEqualTo(draft.getId());
    }

    @Test
    void findByAuthorIdAndByState_returnMatchingDrafts() {
        var exercise = persistExercise();
        var author = persistAccount("byauthor@example.com");
        var draft = draftRepository.saveAndFlush(sampleDraft(exercise, author, Set.of()));
        entityManager.clear();

        assertThat(draftRepository.findByAuthorId(author.getId())).extracting("id").containsExactly(draft.getId());
        assertThat(draftRepository.findByState(DraftState.DRAFT)).extracting("id").containsExactly(draft.getId());
        assertThat(draftRepository.findByState(DraftState.PUBLISHED)).isEmpty();
    }

    @Test
    void existsByExercise_IdAndAuthor_Id_reflectsPersistedState() {
        var exercise = persistExercise();
        var author = persistAccount("exists@example.com");
        var otherAuthor = persistAccount("other@example.com");
        draftRepository.saveAndFlush(sampleDraft(exercise, author, Set.of()));

        assertThat(draftRepository.existsByExercise_IdAndAuthor_Id(exercise.getId(), author.getId())).isTrue();
        assertThat(draftRepository.existsByExercise_IdAndAuthor_Id(exercise.getId(), otherAuthor.getId())).isFalse();
    }
}
