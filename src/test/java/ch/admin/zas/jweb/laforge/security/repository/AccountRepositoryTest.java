package ch.admin.zas.jweb.laforge.security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link AccountRepository}. Vérifie le round-trip complet (y compris la
 * collection {@code roles}) et les requêtes dérivées sur l'email.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var account = new Account("Learner@Example.com", "hash", "Ada Lovelace");
        account.activate();

        var saved = accountRepository.saveAndFlush(account);
        entityManager.clear();

        var reloaded = accountRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("learner@example.com");
        assertThat(reloaded.getPasswordHash()).isEqualTo("hash");
        assertThat(reloaded.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(reloaded.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(Role.LEARNER);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByEmail_returnsAccountRegardlessOfCase() {
        var account = new Account("someone@example.com", "hash", "Someone");
        accountRepository.saveAndFlush(account);
        entityManager.clear();

        Optional<Account> found = accountRepository.findByEmail("someone@example.com");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getDisplayName()).isEqualTo("Someone");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        assertThat(accountRepository.findByEmail("unknown@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_reflectsPersistedState() {
        accountRepository.saveAndFlush(new Account("exists@example.com", "hash", "Someone"));

        assertThat(accountRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(accountRepository.existsByEmail("absent@example.com")).isFalse();
    }

    @Test
    void uniqueEmailConstraint_isEnforcedAtFlush() {
        accountRepository.saveAndFlush(new Account("dup@example.com", "hash", "First"));
        var duplicate = new Account("dup@example.com", "hash2", "Second");

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> accountRepository.saveAndFlush(duplicate));
    }
}
