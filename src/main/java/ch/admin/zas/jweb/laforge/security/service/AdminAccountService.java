package ch.admin.zas.jweb.laforge.security.service;

import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.error.Violation;
import ch.admin.zas.jweb.laforge.common.page.CursorCodec;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.AdminAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.ReplaceAccountRolesInput;
import ch.admin.zas.jweb.laforge.security.mapper.AdminAccountMapper;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.security.repository.AccountRoleLockRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {
    private final AccountRepository accounts;
    private final AccountRoleLockRepository roleLock;

    public AdminAccountService(AccountRepository accounts, AccountRoleLockRepository roleLock) {
        this.accounts = accounts;
        this.roleLock = roleLock;
    }

    @Transactional(readOnly = true)
    public Page<AdminAccountDto> search(CurrentAccountDto actor, String query, PageQuery page) {
        requireAdmin(actor);
        var normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        var fingerprint = CursorCodec.fingerprint(Map.of("query", normalized));
        String afterName = null;
        UUID afterId = null;
        if (page.hasCursor()) {
            var keys = CursorCodec.decode(page.cursor(), fingerprint);
            try {
                if (keys.size() != 2) {
                    throw new IllegalArgumentException();
                }
                afterName = new String(Base64.getUrlDecoder().decode(keys.getFirst()), StandardCharsets.UTF_8);
                afterId = UUID.fromString(keys.getLast());
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Curseur invalide.");
            }
        }
        var pattern = "%" + normalized.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var results = accounts.search(pattern, afterName, afterId, PageRequest.of(0, page.limit() + 1));
        var items = results.stream().limit(page.limit()).map(AdminAccountMapper::toDto).toList();
        var nextCursor = results.size() > page.limit()
                ? CursorCodec.encode(fingerprint, List.of(
                        Base64.getUrlEncoder().withoutPadding().encodeToString(
                                items.getLast().displayName().getBytes(StandardCharsets.UTF_8)),
                        items.getLast().id().toString())) : null;
        return Page.of(items, nextCursor);
    }

    @Transactional
    public AdminAccountDto replaceRoles(CurrentAccountDto actor, UUID accountId, ReplaceAccountRolesInput input) {
        requireAdmin(actor);
        if (actor.id().equals(accountId)) {
            throw new ForbiddenException("Vous ne pouvez pas modifier vos propres rôles.");
        }
        var roles = validatedRoles(input);
        roleLock.lockRoleChanges().orElseThrow(
                () -> new IllegalStateException("Le verrou des changements de rôles est absent."));
        var account = accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Compte introuvable."));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidStateException("Seuls les comptes actifs peuvent recevoir des rôles.");
        }
        if (account.getRoles().contains(Role.ADMIN) && !roles.contains(Role.ADMIN)
                && accounts.countByStatusAndRole(AccountStatus.ACTIVE, Role.ADMIN) <= 1) {
            throw new InvalidStateException("Le dernier administrateur actif doit conserver son rôle.");
        }
        account.replaceRoles(roles);
        return AdminAccountMapper.toDto(account);
    }

    private static void requireAdmin(CurrentAccountDto actor) {
        if (!actor.hasAnyRole(Set.of(Role.ADMIN))) {
            throw new ForbiddenException("Le rôle ADMIN est requis.");
        }
    }

    private static Set<Role> validatedRoles(ReplaceAccountRolesInput input) {
        var roles = EnumSet.noneOf(Role.class);
        if (input == null || input.roles() == null) {
            throw invalidRoles();
        }
        for (var role : input.roles()) {
            if (role == null) {
                throw invalidRoles();
            }
            Role value;
            try {
                value = Role.valueOf(role);
            } catch (IllegalArgumentException exception) {
                throw invalidRoles();
            }
            if (!roles.add(value)) {
                throw invalidRoles();
            }
        }
        if (!roles.contains(Role.LEARNER)) {
            throw invalidRoles();
        }
        return roles;
    }

    private static ValidationFailedException invalidRoles() {
        return new ValidationFailedException("Attributions de rôles invalides.",
                List.of(new Violation("roles", "LEARNER est obligatoire ; seuls LEARNER, AUTHOR et ADMIN sont admis, sans doublon.")));
    }
}
