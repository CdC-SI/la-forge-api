package ch.admin.zas.jweb.laforge.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.profile.dto.UserDto;
import ch.admin.zas.jweb.laforge.profile.service.ProfileService;
import ch.admin.zas.jweb.laforge.profile.web.ProfileController;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProfileController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class CurrentAccountArgumentResolverTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AccountRepository accounts;
    @MockitoBean
    private JwtDecoder decoder;
    @MockitoBean
    private ProfileService profileService;

    @Test
    void me_usesOldJwtPermissionsUntilNewTokenAndIgnoresRetiredReviewer() throws Exception {
        var account = new Account("learner@example.com", "hash", "Ada");
        account.activate();
        account.replaceRoles(Set.of(Role.LEARNER, Role.AUTHOR));
        var id = UUID.randomUUID();
        ReflectionTestUtils.setField(account, "id", id);
        when(accounts.findById(id)).thenReturn(Optional.of(account));
        when(decoder.decode("old")).thenReturn(token("old", id, "LEARNER", "REVIEWER", "ADMIN"));
        when(decoder.decode("renewed")).thenReturn(token("renewed", id, "LEARNER", "AUTHOR"));
        var actor = ArgumentCaptor.forClass(CurrentAccountDto.class);
        when(profileService.getMe(actor.capture())).thenAnswer(invocation -> {
            CurrentAccountDto current = invocation.getArgument(0);
            return new UserDto(current.id(), current.displayName(), current.roles(), null);
        });
        mvc.perform(get("/me").header("Authorization", "Bearer old"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.containsInAnyOrder("LEARNER", "ADMIN")));
        assertThat(actor.getValue().roles()).containsExactlyInAnyOrder(Role.LEARNER, Role.ADMIN);
        mvc.perform(get("/me").header("Authorization", "Bearer renewed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.containsInAnyOrder("LEARNER", "AUTHOR")));
        assertThat(actor.getValue().roles()).containsExactlyInAnyOrder(Role.LEARNER, Role.AUTHOR);
        mvc.perform(get("/me").header("Authorization", "Bearer old"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.containsInAnyOrder("LEARNER", "ADMIN")));
    }

    private static Jwt token(String value, UUID id, String... roles) {
        return Jwt.withTokenValue(value).header("alg", "RS256").subject(id.toString())
                .claim("roles", List.of(roles)).build();
    }
}
