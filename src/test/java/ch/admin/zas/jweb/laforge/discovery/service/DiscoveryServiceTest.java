package ch.admin.zas.jweb.laforge.discovery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.discovery.domain.Article;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleInput;
import ch.admin.zas.jweb.laforge.discovery.dto.ExerciseReferenceDto;
import ch.admin.zas.jweb.laforge.discovery.repository.ArticleRepository;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock private ArticleRepository articles;
    @Mock private TopicRepository topics;
    @Mock private ExerciseVersionRepository versions;
    @Mock private ExerciseCompletionService completionService;

    private DiscoveryService service;
    private ExerciseVersion version;
    private final UUID exerciseId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DiscoveryService(articles, topics, versions, completionService, Clock.systemUTC());
        var exercise = new Exercise();
        ReflectionTestUtils.setField(exercise, "id", exerciseId);
        version = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of());
        version.publish(OffsetDateTime.now());
    }

    @Test
    void relatedExercisesExposeOnlyCurrentLearnersCompletion() {
        var article = article();
        var learner = account();
        var other = account();
        when(articles.findById(articleId)).thenReturn(Optional.of(article));
        when(completionService.completedExerciseIds(learner.id(), List.of(exerciseId))).thenReturn(Set.of(exerciseId));
        when(completionService.completedExerciseIds(other.id(), List.of(exerciseId))).thenReturn(Set.of());

        assertThat(service.getArticle(learner, articleId).relatedExercises().getFirst().completed()).isTrue();
        assertThat(service.getArticle(other, articleId).relatedExercises().getFirst().completed()).isFalse();
        verify(completionService).completedExerciseIds(learner.id(), List.of(exerciseId));
        verify(completionService).completedExerciseIds(other.id(), List.of(exerciseId));
    }

    @Test
    void publicationAndUpdateKeepPersonalCompletionAndMinimalTechnology() {
        var author = account();
        var input = new ArticleInput("Titre", "Résumé", List.of(),
                List.of(new TechnologyRequirement("Java", null, null, null)), "Contenu",
                List.of(), List.of(new ExerciseReferenceDto(exerciseId, 1)));
        when(versions.findByExercise_IdAndVersionNumber(exerciseId, 1)).thenReturn(Optional.of(version));
        when(articles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(completionService.completedExerciseIds(author.id(), List.of(exerciseId))).thenReturn(Set.of(exerciseId));
        var published = service.publishArticle(author, input);
        assertThat(published.relatedExercises().getFirst().completed()).isTrue();
        assertThat(published.technologies()).containsExactly(new TechnologyRequirement("Java", null, null, null));

        when(articles.findById(articleId)).thenReturn(Optional.of(article()));
        assertThat(service.updateArticle(author, articleId, 0, input).relatedExercises().getFirst().completed()).isTrue();
    }

    private Article article() {
        return new Article("Titre", "Résumé", "Contenu", List.of(), List.of(), Set.of(),
                Set.of(version), OffsetDateTime.now());
    }

    private CurrentAccountDto account() {
        return new CurrentAccountDto(UUID.randomUUID(), "user@example.com", "Utilisateur",
                AccountStatus.ACTIVE, Set.of(Role.LEARNER));
    }
}
