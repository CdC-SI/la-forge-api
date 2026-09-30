package ch.admin.zas.jweb.laforge.security.web;

import ch.admin.zas.jweb.laforge.security.dto.LoginInput;
import ch.admin.zas.jweb.laforge.security.dto.RefreshRequest;
import ch.admin.zas.jweb.laforge.security.dto.RegistrationInput;
import ch.admin.zas.jweb.laforge.security.dto.ResendVerificationInput;
import ch.admin.zas.jweb.laforge.security.dto.SessionResponse;
import ch.admin.zas.jweb.laforge.security.dto.VerificationInput;
import ch.admin.zas.jweb.laforge.security.service.AccountVerificationService;
import ch.admin.zas.jweb.laforge.security.service.AuthenticationService;
import ch.admin.zas.jweb.laforge.security.service.RefreshTokenService;
import ch.admin.zas.jweb.laforge.security.service.RegistrationService;
import ch.admin.zas.jweb.laforge.security.service.TokenIssuer;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.common.ratelimit.RateLimited;
import ch.admin.zas.jweb.laforge.common.ratelimit.RateLimitFamily;
import ch.admin.zas.jweb.laforge.profile.dto.UserDto;
import ch.admin.zas.jweb.laforge.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inscription, confirmation de compte et cycle de vie de session (tag {@code Auth} du contrat).
 * Toutes les routes sont publiques ({@code security: []}) : {@link SecurityConfig} les exclut de
 * l'authentification par porteur JWT.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AccountVerificationService accountVerificationService;
    private final AuthenticationService authenticationService;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final ProfileService profileService;

    public AuthController(
            RegistrationService registrationService,
            AccountVerificationService accountVerificationService,
            AuthenticationService authenticationService,
            TokenIssuer tokenIssuer,
            RefreshTokenService refreshTokenService,
            ProfileService profileService) {
        this.registrationService = registrationService;
        this.accountVerificationService = accountVerificationService;
        this.authenticationService = authenticationService;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.profileService = profileService;
    }

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimited(RateLimitFamily.AUTH)
    public void registerAccount(@Valid @RequestBody RegistrationInput input) {
        registrationService.register(input.email(), input.password(), input.displayName());
    }

    @PostMapping("/verifications")
    @RateLimited(RateLimitFamily.AUTH)
    public UserDto verifyAccount(@Valid @RequestBody VerificationInput input) {
        var account = accountVerificationService.verify(input.token());
        return profileService.getMe(CurrentAccountDto.from(account));
    }

    @PostMapping("/verifications/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimited(RateLimitFamily.AUTH)
    public void resendVerification(@Valid @RequestBody ResendVerificationInput input) {
        registrationService.resendVerification(input.email());
    }

    @PostMapping("/sessions")
    @RateLimited(RateLimitFamily.AUTH)
    public SessionResponse createSession(@Valid @RequestBody LoginInput input) {
        var account = authenticationService.authenticate(input.email(), input.password());
        return session(account);
    }

    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @PostMapping("/sessions/refresh")
    @RateLimited(RateLimitFamily.AUTH)
    public SessionResponse refreshSession(@Valid @RequestBody RefreshRequest request) {
        var issued = refreshTokenService.rotate(request.refreshToken());
        return session(issued.account(), issued.rawToken());
    }

    private SessionResponse session(Account account) {
        var issued = refreshTokenService.issueNewFamily(account);
        return session(account, issued.rawToken());
    }

    private SessionResponse session(Account account, String refreshToken) {
        var accessToken = tokenIssuer.issueAccessToken(account);
        return SessionResponse.bearer(
                accessToken,
                refreshToken,
                tokenIssuer.accessTokenTtl().toSeconds(),
                profileService.getMe(CurrentAccountDto.from(account)));
    }
}
