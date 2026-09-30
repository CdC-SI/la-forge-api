package ch.admin.zas.jweb.laforge.security.mapper;

import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.AdminAccountDto;

public final class AdminAccountMapper {
    private AdminAccountMapper() {
    }

    public static AdminAccountDto toDto(Account account) {
        return new AdminAccountDto(account.getId(), account.getEmail(), account.getDisplayName(),
                account.getStatus(), account.getRoles());
    }
}
