package ch.admin.zas.jweb.laforge.security.dto;

import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.util.Set;
import java.util.UUID;

/** Identité administrative et attributions qui seront utilisées par les prochains jetons. */
public record AdminAccountDto(UUID id, String email, String displayName, AccountStatus status, Set<Role> roles) {
    public AdminAccountDto {
        roles = Set.copyOf(roles);
    }
}
