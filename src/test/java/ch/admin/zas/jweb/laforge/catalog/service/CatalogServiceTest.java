package ch.admin.zas.jweb.laforge.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicInput;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests unitaires de la création de thèmes par {@link CatalogService}. */
@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private ExerciseVersionRepository exerciseVersionRepository;

    private CatalogService service;

    @BeforeEach
    void setUp() {
        service = new CatalogService(topicRepository, exerciseVersionRepository);
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
}
