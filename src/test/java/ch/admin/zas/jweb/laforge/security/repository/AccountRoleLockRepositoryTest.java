package ch.admin.zas.jweb.laforge.security.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.ReplaceAccountRolesInput;
import ch.admin.zas.jweb.laforge.security.service.AdminAccountService;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Vérifie la sérialisation réelle des transactions sur H2, indépendamment des migrations PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountRoleLockRepositoryTest {
    @Autowired
    private AccountRoleLockRepository roleLock;
    @Autowired
    private AccountRepository accounts;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        jdbc.update("""
                insert into account_role_lock (id)
                select 1 where not exists (select 1 from account_role_lock where id = 1)
                """);
    }

    @AfterEach
    void cleanUp() {
        accounts.deleteAll();
        roleLock.deleteAll();
    }

    @Test
    void singletonLock_blocksCompetingTransactionUntilCommit() throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transaction.executeWithoutResult(status -> {
                roleLock.lockRoleChanges().orElseThrow();
                locked.countDown();
                await(release);
            }));
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> transaction.execute(status -> {
                    secondStarted.countDown();
                    return roleLock.lockRoleChanges().isPresent();
                }));
                assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> second.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                first.get(10, TimeUnit.SECONDS);
                assertThat(second.get(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void simultaneousDemotions_leaveOneActiveAdmin() throws Exception {
        var first = createAdmin("first@example.com");
        var second = createAdmin("second@example.com");
        var service = new AdminAccountService(accounts, roleLock);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var demoteFirst = executor.submit(() -> demote(service, first, ready, start));
            var demoteSecond = executor.submit(() -> demote(service, second, ready, start));
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            assertThat(java.util.List.of(demoteFirst.get(10, TimeUnit.SECONDS), demoteSecond.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(accounts.countByStatusAndRole(AccountStatus.ACTIVE, Role.ADMIN)).isEqualTo(1);
    }

    private boolean demote(AdminAccountService service, UUID targetId, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        await(start);
        // Un JWT ADMIN encore valide peut appartenir à un compte déjà rétrogradé.
        var actor = new CurrentAccountDto(UUID.randomUUID(), "actor@example.com", "Acteur",
                AccountStatus.ACTIVE, Set.of(Role.LEARNER, Role.ADMIN));
        try {
            transaction.executeWithoutResult(status ->
                    service.replaceRoles(actor, targetId, new ReplaceAccountRolesInput(java.util.List.of("LEARNER"))));
            return true;
        } catch (InvalidStateException exception) {
            return false;
        }
    }

    private UUID createAdmin(String email) {
        var account = new Account(email, "hash", "Admin");
        account.activate();
        account.replaceRoles(Set.of(Role.LEARNER, Role.ADMIN));
        return accounts.saveAndFlush(account).getId();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Délai de synchronisation dépassé.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
