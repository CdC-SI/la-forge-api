package ch.admin.zas.jweb.laforge.security.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.profile.service.ProfileService;
import ch.admin.zas.jweb.laforge.security.service.AccountVerificationService;
import ch.admin.zas.jweb.laforge.security.service.AuthenticationService;
import ch.admin.zas.jweb.laforge.security.service.RefreshTokenService;
import ch.admin.zas.jweb.laforge.security.service.RegistrationService;
import ch.admin.zas.jweb.laforge.security.service.TokenIssuer;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de contrat pour {@link AuthController}. Toutes les routes sont publiques
 * ({@code /auth/**} exclu de l'authentification par {@link SecurityConfig}) : la configuration de
 * sécurité réelle est tout de même importée (au lieu du filtre par défaut de Spring Boot, qui
 * activerait CSRF et bloquerait ces POST publics par 403) pour refléter fidèlement le comportement
 * de production ; {@link JwtDecoder} est mocké car requis par le bean {@code securityFilterChain}.
 */
@WebMvcTest(controllers = AuthController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private AccountVerificationService accountVerificationService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private TokenIssuer tokenIssuer;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private ProfileService profileService;

    /**
     * Non utilisé par {@link AuthController} (aucune route {@code @CurrentAccount}), mais requis
     * car {@code CurrentAccountArgumentResolver} est un {@code HandlerMethodArgumentResolver}
     * chargé par tout slice {@code @WebMvcTest}, quel que soit le contrôleur ciblé.
     */
    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void registerAccount_validBody_returns202() throws Exception {
        var body = """
                {"email":"ada@example.com","password":"une-phrase-secrete","displayName":"Ada"}
                """;

        mockMvc.perform(post("/auth/registrations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());

        verify(registrationService).register("ada@example.com", "une-phrase-secrete", "Ada");
    }

    @Test
    void registerAccount_invalidEmail_isUnprocessable() throws Exception {
        var body = """
                {"email":"pas-un-email","password":"une-phrase-secrete","displayName":"Ada"}
                """;

        mockMvc.perform(post("/auth/registrations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createSession_accountNotActive_isForbidden() throws Exception {
        doThrow(new ForbiddenException("Le compte n'est pas encore activé."))
                .when(authenticationService).authenticate(any(), any());
        var body = """
                {"email":"ada@example.com","password":"une-phrase-secrete"}
                """;

        mockMvc.perform(post("/auth/sessions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
