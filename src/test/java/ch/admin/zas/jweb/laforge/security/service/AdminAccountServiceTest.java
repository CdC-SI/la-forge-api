package ch.admin.zas.jweb.laforge.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountRoleLock;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.ReplaceAccountRolesInput;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.security.repository.AccountRoleLockRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {
    @Mock
    private AccountRepository accounts;
    @Mock
    private AccountRoleLockRepository roleLock;
    private AdminAccountService service;
    private final CurrentAccountDto admin = new CurrentAccountDto(UUID.randomUUID(), "admin@test.ch", "Admin",
            AccountStatus.ACTIVE, Set.of(Role.LEARNER, Role.ADMIN));

    @BeforeEach
    void setUp() {
        service = new AdminAccountService(accounts, roleLock);
    }

    @Test
    void search_normalizesAndEscapesQueryAndBuildsBoundCursor() {
        var first = account(Set.of(Role.LEARNER));
        var second = account(Set.of(Role.LEARNER));
        when(accounts.search(eq("%a!%!_!!%"), eq(null), eq(null), any())).thenReturn(List.of(first, second));
        var page = service.search(admin, " A%_! ", new PageQuery(1, null));
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().getFirst().id()).isEqualTo(first.getId());
        assertThat(CursorCodec.decode(page.nextCursor(), CursorCodec.fingerprint(Map.of("query", "a%_!"))))
                .containsExactly("Q2libGU", first.getId().toString());
        verify(accounts).search("%a!%!_!!%", null, null, PageRequest.of(0, 2));
        when(accounts.search(eq("%a!%!_!!%"), eq(first.getDisplayName()), eq(first.getId()), any()))
                .thenReturn(List.of(second));
        assertThat(service.search(admin, "a%_!", new PageQuery(1, page.nextCursor())).nextCursor()).isNull();
        assertThatThrownBy(() -> service.search(admin, "different", new PageQuery(1, page.nextCursor())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void search_rejectsInvalidCursor() {
        assertThatThrownBy(() -> service.search(admin, null, new PageQuery(10, "invalid")))
                .isInstanceOf(BadRequestException.class);
        var cursor = CursorCodec.encode(CursorCodec.fingerprint(Map.of("query", "")), List.of("Q2libGU", "not-a-uuid"));
        assertThatThrownBy(() -> service.search(admin, null, new PageQuery(10, cursor)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(accounts);
    }

    @Test
    void search_cursorPreservesDisplayNamesWithCommasAndUnicode() {
        var first = account(Set.of(Role.LEARNER));
        first.rename("Émile, Z");
        var second = account(Set.of(Role.LEARNER));
        when(accounts.search(eq("%%"), eq(null), eq(null), any())).thenReturn(List.of(first, second));
        var page = service.search(admin, null, new PageQuery(1, null));
        when(accounts.search(eq("%%"), eq("Émile, Z"), eq(first.getId()), any())).thenReturn(List.of(second));
        assertThat(service.search(admin, null, new PageQuery(1, page.nextCursor())).items()).hasSize(1);
        verify(accounts).search("%%", "Émile, Z", first.getId(), PageRequest.of(0, 2));
    }

    @Test
    void operations_useEffectiveActorRolesNotDatabaseRoles() {
        var learner = new CurrentAccountDto(admin.id(), admin.email(), admin.displayName(), admin.status(),
                Set.of(Role.LEARNER));
        assertThatThrownBy(() -> service.search(learner, null, new PageQuery(20, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.replaceRoles(learner, UUID.randomUUID(), input("LEARNER")))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(accounts, roleLock);
    }

    @Test
    void replaceRoles_rejectsSelfModificationBeforeLock() {
        assertThatThrownBy(() -> service.replaceRoles(admin, admin.id(), input("LEARNER")))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(accounts, roleLock);
    }

    @Test
    void replaceRoles_rejectsInvalidRolesBeforeLock() {
        for (var input : List.of(input(), input("ADMIN"), input("LEARNER", "REVIEWER"), input("LEARNER", "LEARNER"),
                input("LEARNER", "unknown"), new ReplaceAccountRolesInput(null),
                new ReplaceAccountRolesInput(java.util.Arrays.asList("LEARNER", null)))) {
            assertThatThrownBy(() -> service.replaceRoles(admin, UUID.randomUUID(), input))
                    .isInstanceOf(ValidationFailedException.class);
        }
        verifyNoInteractions(accounts, roleLock);
    }

    @Test
    void replaceRoles_missingAccountIsNotFound() {
        lock();
        var id = UUID.randomUUID();
        when(accounts.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.replaceRoles(admin, id, input("LEARNER")))
                .isInstanceOf(NotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"PENDING_VERIFICATION", "DISABLED", "LOCKED"})
    void replaceRoles_rejectsInactiveAccount(AccountStatus status) {
        var target = account(Set.of(Role.LEARNER));
        ReflectionTestUtils.setField(target, "status", status);
        target(target);
        assertThatThrownBy(() -> service.replaceRoles(admin, target.getId(), input("LEARNER", "AUTHOR")))
                .isInstanceOf(InvalidStateException.class);
        assertThat(target.getRoles()).containsExactly(Role.LEARNER);
    }

    @Test
    void replaceRoles_protectsLastActiveAdminInsideGlobalLock() {
        var target = account(Set.of(Role.LEARNER, Role.ADMIN));
        target(target);
        when(accounts.countByStatusAndRole(AccountStatus.ACTIVE, Role.ADMIN)).thenReturn(1L);
        assertThatThrownBy(() -> service.replaceRoles(admin, target.getId(), input("LEARNER")))
                .isInstanceOf(InvalidStateException.class);
        assertThat(target.getRoles()).contains(Role.ADMIN);
        var ordered = inOrder(roleLock, accounts);
        ordered.verify(roleLock).lockRoleChanges();
        ordered.verify(accounts).findById(target.getId());
        ordered.verify(accounts).countByStatusAndRole(AccountStatus.ACTIVE, Role.ADMIN);
    }

    @Test
    void replaceRoles_allowsDemotionWhenAnotherAdminRemains() {
        var target = account(Set.of(Role.LEARNER, Role.ADMIN));
        target(target);
        when(accounts.countByStatusAndRole(AccountStatus.ACTIVE, Role.ADMIN)).thenReturn(2L);
        assertThat(service.replaceRoles(admin, target.getId(), input("LEARNER")).roles())
                .containsExactly(Role.LEARNER);
    }

    @Test
    void replaceRoles_isIdempotentAndLastWriteWins() {
        var target = account(Set.of(Role.LEARNER));
        target(target);
        var first = service.replaceRoles(admin, target.getId(), input("LEARNER", "AUTHOR"));
        assertThat(service.replaceRoles(admin, target.getId(), input("LEARNER", "AUTHOR"))).isEqualTo(first);
        assertThat(service.replaceRoles(admin, target.getId(), input("LEARNER")).roles())
                .containsExactly(Role.LEARNER);
    }

    @Test
    void replaceRoles_keepsLastAdminWhenRoleRetained() {
        var target = account(Set.of(Role.LEARNER, Role.ADMIN));
        target(target);
        assertThat(service.replaceRoles(admin, target.getId(), input("LEARNER", "ADMIN")).roles())
                .containsExactlyInAnyOrder(Role.LEARNER, Role.ADMIN);
    }

    private void target(Account target) {
        lock();
        when(accounts.findById(target.getId())).thenReturn(Optional.of(target));
    }

    private void lock() {
        when(roleLock.lockRoleChanges()).thenReturn(Optional.of(mock(AccountRoleLock.class)));
    }

    private static ReplaceAccountRolesInput input(String... roles) {
        return new ReplaceAccountRolesInput(List.of(roles));
    }

    private static Account account(Set<Role> roles) {
        var account = new Account(UUID.randomUUID() + "@test.ch", "secret-hash", "Cible");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.activate();
        account.replaceRoles(roles);
        return account;
    }
}
