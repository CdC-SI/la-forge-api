package ch.admin.zas.jweb.laforge.profile.dto;

import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.profile.domain.Preferences;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Préférences de pratique de l'apprenant (schéma {@code Preferences} du contrat).
 *
 * <p>Tant qu'aucune préférence n'a été enregistrée (aucun {@code PUT /me/preferences} effectué),
 * {@link #defaultPreferences()} fournit les valeurs par défaut d'un compte fraîchement activé,
 * sans qu'une ligne ne soit persistée pour autant.
 */
public record PreferencesDto(
        @Valid @Size(max = 20) List<StackEntryDto> stack,
        @NotNull @Size(max = 50) List<UUID> topicIds,
        @NotNull Difficulty difficulty,
        @Min(5) @Max(30) int sessionMinutes,
        @Pattern(regexp = "^(fr|en)$") String locale,
        @Size(max = 100) String timeZone) {

    public static PreferencesDto defaultPreferences() {
        return new PreferencesDto(
                List.of(),
                List.of(),
                Difficulty.INTERMEDIATE,
                Preferences.DEFAULT_SESSION_MINUTES,
                Preferences.DEFAULT_LOCALE,
                Preferences.DEFAULT_TIME_ZONE);
    }

    public static PreferencesDto from(Preferences preferences) {
        var topicIds = preferences.getTopics().stream().map(topic -> topic.getId()).collect(Collectors.toList());
        return new PreferencesDto(
                preferences.getStack(),
                topicIds,
                preferences.getDifficulty(),
                preferences.getSessionMinutes(),
                preferences.getLocale(),
                preferences.getTimeZone());
    }
}
