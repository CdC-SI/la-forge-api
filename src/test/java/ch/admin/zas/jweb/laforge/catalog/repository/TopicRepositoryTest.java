package ch.admin.zas.jweb.laforge.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** Test de persistance de {@link TopicRepository} : round-trip simple d'un référentiel. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class TopicRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var topic = new Topic("java", "Java");

        var saved = topicRepository.saveAndFlush(topic);
        entityManager.clear();

        var reloaded = topicRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getSlug()).isEqualTo("java");
        assertThat(reloaded.getLabel()).isEqualTo("Java");
    }

    @Test
    void uniqueSlugConstraint_isEnforcedAtFlush() {
        topicRepository.saveAndFlush(new Topic("angular", "Angular"));
        var duplicate = new Topic("angular", "Angular (bis)");

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class, () -> topicRepository.saveAndFlush(duplicate));
    }
}
