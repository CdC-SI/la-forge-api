package ch.admin.zas.jweb.laforge.security.dto;

import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.util.Set;
import java.util.UUID;

/** Identité du compte authentifié exposée aux contrôleurs, sans dépendance envers l'entité JPA. */
public record CurrentAccountDto(
        UUID id,
        String email,
        String displayName,
        AccountStatus status,
        Set<Role> roles) {

    public CurrentAccountDto {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static CurrentAccountDto from(Account account) {
        return new CurrentAccountDto(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getStatus(),
                account.getRoles());
    }

    public boolean hasAnyRole(Set<Role> requiredRoles) {
        return !java.util.Collections.disjoint(roles, requiredRoles);
    }
}
