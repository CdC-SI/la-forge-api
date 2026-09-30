package ch.admin.zas.jweb.laforge.security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

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

    @Autowired
    private AccountRoleLockRepository roleLockRepository;

    @Test
    void roleReplacement_roundTripsAndCountsOnlyActiveAdmins() {
        var active = new Account("admin@example.com", "hash", "Admin");
        active.activate();
        active.replaceRoles(Set.of(Role.LEARNER, Role.ADMIN, Role.AUTHOR));
        accountRepository.saveAndFlush(active);
        var disabled = new Account("disabled@example.com", "hash", "Disabled");
        disabled.activate();
        disabled.replaceRoles(Set.of(Role.LEARNER, Role.ADMIN));
        org.springframework.test.util.ReflectionTestUtils.setField(disabled, "status", AccountStatus.DISABLED);
        accountRepository.saveAndFlush(disabled);
        entityManager.clear();
        assertThat(accountRepository.countByStatusAndRole(AccountStatus.ACTIVE, Role.ADMIN)).isEqualTo(1);
        var reloaded = accountRepository.findById(active.getId()).orElseThrow();
        assertThat(reloaded.getRoles()).containsExactlyInAnyOrder(Role.LEARNER, Role.AUTHOR, Role.ADMIN);
        reloaded.replaceRoles(Set.of(Role.LEARNER));
        entityManager.flush();
        entityManager.clear();
        assertThat(accountRepository.findById(active.getId()).orElseThrow().getRoles()).containsExactly(Role.LEARNER);
        assertThat(accountRepository.countByStatusAndRole(AccountStatus.ACTIVE, Role.ADMIN)).isZero();
    }

    @Test
    void search_matchesNameOrEmailWithStableKeysetAndLiteralWildcards() {
        var first = accountRepository.saveAndFlush(new Account("ada@example.com", "hash", "One"));
        var second = accountRepository.saveAndFlush(new Account("second@example.com", "hash", "ADA Lovelace"));
        var literal = accountRepository.saveAndFlush(new Account("third@example.com", "hash", "100%_!"));
        assertThat(accountRepository.search("%ada%", null, null, PageRequest.of(0, 10)))
                .extracting(Account::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
        var all = accountRepository.search("%", null, null, PageRequest.of(0, 10));
        assertThat(all).hasSize(3);
        assertThat(accountRepository.search("%", all.getFirst().getDisplayName(), all.getFirst().getId(), PageRequest.of(0, 10)))
                .extracting(Account::getId).containsExactly(all.get(1).getId(), all.get(2).getId());
        assertThat(accountRepository.search("%!%!_!!%", null, null, PageRequest.of(0, 10)))
                .extracting(Account::getId).containsExactly(literal.getId());
    }

    @Test
    void search_ordersByDisplayNameThenIdEvenWithIdenticalNames() {
        accountRepository.saveAndFlush(new Account("z@example.com", "hash", "Zelda"));
        accountRepository.saveAndFlush(new Account("ada1@example.com", "hash", "Ada"));
        accountRepository.saveAndFlush(new Account("ada2@example.com", "hash", "Ada"));
        var firstPage = accountRepository.search("%", null, null, PageRequest.of(0, 1));
        assertThat(firstPage.getFirst().getDisplayName()).isEqualTo("Ada");
        var rest = accountRepository.search("%", "Ada", firstPage.getFirst().getId(), PageRequest.of(0, 10));
        assertThat(rest).extracting(Account::getDisplayName).containsExactly("Ada", "Zelda");
        assertThat(rest).extracting(Account::getId).doesNotContain(firstPage.getFirst().getId());
    }

    @Test
    void singletonRoleLock_canBeAcquiredInTransaction() {
        entityManager.getEntityManager().createNativeQuery("insert into account_role_lock (id) values (1)")
                .executeUpdate();
        assertThat(roleLockRepository.lockRoleChanges()).isPresent();
    }

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
