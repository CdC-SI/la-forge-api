package ch.admin.zas.jweb.laforge.catalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/** Test de persistance de {@link ExerciseRepository} : l'entité ne porte aucun champ propre. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class ExerciseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Test
    void saveThenFindById_generatesAuditedIdentity() {
        var saved = exerciseRepository.saveAndFlush(new Exercise());
        entityManager.clear();

        var reloaded = exerciseRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }
}
