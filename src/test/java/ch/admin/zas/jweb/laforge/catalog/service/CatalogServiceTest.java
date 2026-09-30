package ch.admin.zas.jweb.laforge.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicInput;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests unitaires de la création de thèmes par {@link CatalogService}. */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private ExerciseVersionRepository exerciseVersionRepository;
    @Mock
    private ExerciseCompletionService exerciseCompletionService;

    private CatalogService service;

    @BeforeEach
    void setUp() {
        service = new CatalogService(topicRepository, exerciseVersionRepository, exerciseCompletionService);
    }

    @Test
    void createTopic_retourneLeThemeAvecSonIdentifiantGenere() {
        var generatedId = UUID.randomUUID();
        when(topicRepository.saveAndFlush(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            ReflectionTestUtils.setField(topic, "id", generatedId);
            return topic;
        });

        var dto = service.createTopic(new TopicInput("java-streams", "Flux Java"));

        assertThat(dto.id()).isEqualTo(generatedId);
        assertThat(dto.slug()).isEqualTo("java-streams");
        assertThat(dto.label()).isEqualTo("Flux Java");
    }

    @Test
    void createTopic_slugDejaUtilise_estUnConflit() {
        when(topicRepository.existsBySlug("java")).thenReturn(true);

        assertThrows(InvalidStateException.class, () -> service.createTopic(new TopicInput("java", "Java")));
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTopic_insertionConcurrente_estUnConflit() {
        when(topicRepository.saveAndFlush(any(Topic.class))).thenThrow(new DataIntegrityViolationException("uk_topic_slug"));

        assertThrows(InvalidStateException.class, () -> service.createTopic(new TopicInput("java", "Java")));
    }

    @Test
    void listExercises_calculeLaCompletionEnUnLotPourLaPageSansChangerLeCurseur() {
        var account = currentAccount();
        var first = publishedVersion(newExercise(), 2);
        var second = publishedVersion(newExercise(), 1);
        var next = publishedVersion(newExercise(), 1);
        when(exerciseVersionRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second, next)));
        var ids = List.of(first.getExercise().getId(), second.getExercise().getId());
        when(exerciseCompletionService.completedExerciseIds(account.id(), ids)).thenReturn(Set.of(ids.getFirst()));

        var page = service.listExercises(account, new PageQuery(2, null), null, null, null, null, null);

        assertThat(page.items()).extracting(item -> item.completed()).containsExactly(true, false);
        assertThat(page.items()).extracting(item -> item.id()).containsExactlyElementsOf(ids);
        assertThat(page.nextCursor()).isNotBlank();
        verify(exerciseCompletionService).completedExerciseIds(account.id(), ids);
    }

    @Test
    void detailEtAncienneVersion_utilisentLExerciceEtLeCompteCourant() {
        var account = currentAccount();
        var other = currentAccount();
        var exercise = newExercise();
        var latest = publishedVersion(exercise, 2);
        var old = publishedVersion(exercise, 1);
        when(exerciseVersionRepository.findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(exercise.getId()))
                .thenReturn(Optional.of(latest));
        when(exerciseVersionRepository.findByExercise_IdAndVersionNumber(exercise.getId(), 1))
                .thenReturn(Optional.of(old));
        when(exerciseCompletionService.completedExerciseIds(account.id(), List.of(exercise.getId())))
                .thenReturn(Set.of(exercise.getId()));
        when(exerciseCompletionService.completedExerciseIds(other.id(), List.of(exercise.getId())))
                .thenReturn(Set.of());

        assertThat(service.getLatestExercise(account, exercise.getId()).completed()).isTrue();
        assertThat(service.getExerciseVersion(account, exercise.getId(), 1).completed()).isTrue();
        assertThat(service.getLatestExercise(other, exercise.getId()).completed()).isFalse();
    }

    private static CurrentAccountDto currentAccount() {
        var account = new Account(UUID.randomUUID() + "@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return CurrentAccountDto.from(account);
    }

    private static Exercise newExercise() {
        var exercise = new Exercise();
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        return exercise;
    }

    private static ExerciseVersion publishedVersion(Exercise exercise, int number) {
        var version = ExerciseVersionFixtures.sampleExerciseVersion(exercise, number, Set.of());
        ReflectionTestUtils.setField(version, "id", UUID.randomUUID());
        version.publish(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return version;
    }
}
