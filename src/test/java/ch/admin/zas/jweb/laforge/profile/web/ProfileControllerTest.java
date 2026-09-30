package ch.admin.zas.jweb.laforge.profile.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.profile.dto.PreferencesDto;
import ch.admin.zas.jweb.laforge.profile.dto.UserDto;
import ch.admin.zas.jweb.laforge.profile.service.ProfileService;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
 * Tests de contrat pour {@link ProfileController} : toutes les routes lisent le compte courant via
 * {@code @CurrentAccount}, résolu à partir du claim {@code sub} du porteur JWT.
 */
@WebMvcTest(controllers = ProfileController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class ProfileControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountRepository accountRepository;

    private void stubAccount() {
        var account = org.mockito.Mockito.mock(Account.class);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
    }

    @Test
    void getMe_success() throws Exception {
        stubAccount();
        var dto = new UserDto(ACCOUNT_ID, "Ada", Set.of(Role.LEARNER), PreferencesDto.defaultPreferences());
        when(profileService.getMe(any(Account.class))).thenReturn(dto);

        mockMvc.perform(get("/me").with(jwt().jwt(builder -> builder.subject(ACCOUNT_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.displayName").value("Ada"));
    }

    @Test
    void getMe_withoutJwt_isUnauthenticated() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void replaceMyPreferences_invalidBody_isUnprocessable() throws Exception {
        stubAccount();
        var invalidBody = """
                {"stack":[],"topicIds":[],"difficulty":"INTERMEDIATE","sessionMinutes":1,"locale":"fr","timeZone":"Europe/Zurich"}
                """;

        mockMvc.perform(put("/me/preferences")
                        .with(jwt().jwt(builder -> builder.subject(ACCOUNT_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.violations[0].field").value("sessionMinutes"));
    }
}
