package ch.admin.zas.jweb.laforge.profile.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.profile.domain.Preferences;
import ch.admin.zas.jweb.laforge.profile.dto.StackEntryDto;
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
 * Tests de persistance de {@link PreferencesRepository} : round-trip du sous-document {@code
 * jsonb} {@code stack}, de la relation {@code ManyToMany} vers {@link Topic} et de la recherche
 * par compte.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class PreferencesRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PreferencesRepository preferencesRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    @Test
    void saveThenFindById_roundTripsDefaultValues() {
        var account = persistAccount("defaults@example.com");
        var preferences = new Preferences(account);

        var saved = preferencesRepository.saveAndFlush(preferences);
        entityManager.clear();

        var reloaded = preferencesRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAccount().getId()).isEqualTo(account.getId());
        assertThat(reloaded.getStack()).isEmpty();
        assertThat(reloaded.getDifficulty()).isEqualTo(Difficulty.INTERMEDIATE);
        assertThat(reloaded.getSessionMinutes()).isEqualTo(Preferences.DEFAULT_SESSION_MINUTES);
        assertThat(reloaded.getLocale()).isEqualTo(Preferences.DEFAULT_LOCALE);
        assertThat(reloaded.getTimeZone()).isEqualTo(Preferences.DEFAULT_TIME_ZONE);
    }

    @Test
    void replaceWith_roundTripsStackAndTopics() {
        var account = persistAccount("replace@example.com");
        var topic = entityManager.persistAndFlush(new Topic("angular", "Angular"));
        var preferences = preferencesRepository.saveAndFlush(new Preferences(account));
        preferences.replaceWith(
                List.of(new StackEntryDto("Angular", "17", "18")),
                new LinkedHashSet<>(Set.of(topic)),
                Difficulty.ADVANCED,
                30,
                "en",
                "UTC");
        preferencesRepository.saveAndFlush(preferences);
        entityManager.clear();

        var reloaded = preferencesRepository.findById(preferences.getId()).orElseThrow();
        assertThat(reloaded.getStack()).hasSize(1);
        assertThat(reloaded.getStack().get(0).technology()).isEqualTo("Angular");
        assertThat(reloaded.getStack().get(0).targetVersion()).isEqualTo("18");
        assertThat(reloaded.getTopics()).extracting("id").containsExactly(topic.getId());
        assertThat(reloaded.getDifficulty()).isEqualTo(Difficulty.ADVANCED);
        assertThat(reloaded.getSessionMinutes()).isEqualTo(30);
        assertThat(reloaded.getLocale()).isEqualTo("en");
        assertThat(reloaded.getTimeZone()).isEqualTo("UTC");
    }

    @Test
    void findByAccount_Id_returnsAssociatedPreferences() {
        var account = persistAccount("lookup@example.com");
        preferencesRepository.saveAndFlush(new Preferences(account));

        assertThat(preferencesRepository.findByAccount_Id(account.getId())).isPresent();
        assertThat(preferencesRepository.findByAccount_Id(java.util.UUID.randomUUID())).isEmpty();
    }
}
