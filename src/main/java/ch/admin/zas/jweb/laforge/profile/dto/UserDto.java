package ch.admin.zas.jweb.laforge.profile.dto;

import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.util.Set;
import java.util.UUID;

/** Représentation publique d'un compte (schéma {@code User} du contrat). */
public record UserDto(UUID id, String displayName, Set<Role> roles, PreferencesDto preferences) {

    /** Construit la vue publique d'un compte avec ses préférences réelles (ou par défaut). */
    public static UserDto of(Account account, PreferencesDto preferences) {
        return new UserDto(account.getId(), account.getDisplayName(), account.getRoles(), preferences);
    }

    /** Variante sans préférences persistées connues (ex. juste après inscription/vérification). */
    public static UserDto from(Account account) {
        return of(account, PreferencesDto.defaultPreferences());
    }
}
