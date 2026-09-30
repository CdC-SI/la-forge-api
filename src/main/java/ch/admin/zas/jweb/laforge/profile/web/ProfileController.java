package ch.admin.zas.jweb.laforge.profile.web;

import ch.admin.zas.jweb.laforge.profile.dto.DashboardDto;
import ch.admin.zas.jweb.laforge.profile.dto.PreferencesDto;
import ch.admin.zas.jweb.laforge.profile.dto.ProgressDto;
import ch.admin.zas.jweb.laforge.profile.dto.UserDto;
import ch.admin.zas.jweb.laforge.profile.service.ProfileService;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.web.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Profil de l'utilisateur courant : identité, préférences, tableau de bord et progression (tag {@code Profile}). */
@RestController
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public UserDto getMe(@CurrentAccount CurrentAccountDto account) {
        return profileService.getMe(account);
    }

    @PutMapping("/me/preferences")
    public PreferencesDto replaceMyPreferences(@CurrentAccount CurrentAccountDto account, @Valid @RequestBody PreferencesDto input) {
        return profileService.replacePreferences(account, input);
    }

    @GetMapping("/me/dashboard")
    public DashboardDto getMyDashboard(@CurrentAccount CurrentAccountDto account) {
        return profileService.getDashboard(account);
    }

    @GetMapping("/me/progress")
    public ProgressDto getMyProgress(@CurrentAccount CurrentAccountDto account) {
        return profileService.getProgress(account);
    }
}
