package ch.admin.zas.jweb.laforge.security.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.AdminAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.ReplaceAccountRolesInput;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.security.service.AdminAccountService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = AdminAccountController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class AdminAccountControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AdminAccountService service;
    @MockitoBean
    private AccountRepository accounts;
    @MockitoBean
    private JwtDecoder decoder;
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @Test
    void routes_requireAuthentication() throws Exception {
        mvc.perform(get("/admin/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(put("/admin/accounts/{id}/roles", targetId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"LEARNER\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPaginationAndAccountIdReturn400() throws Exception {
        authenticate("ADMIN");
        mvc.perform(get("/admin/accounts").header("Authorization", "Bearer token")
                        .param("limit", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(put("/admin/accounts/{id}/roles", "invalid").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"LEARNER\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEARNER", "AUTHOR", "REVIEWER"})
    void routes_requireAdminAuthority(String role) throws Exception {
        authenticate(role);
        mvc.perform(get("/admin/accounts").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/admin/accounts/{id}/roles", targetId).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"LEARNER\"]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void search_exposesOnlyAdministrativeFieldsAndUsesJwtRoles() throws Exception {
        authenticate("LEARNER", "ADMIN", "REVIEWER");
        var dto = new AdminAccountDto(targetId, "target@example.com", "Cible", AccountStatus.ACTIVE,
                Set.of(Role.LEARNER));
        when(service.search(any(), eq("cible"), eq(new PageQuery(2, "opaque"))))
                .thenReturn(new Page<>(List.of(dto), "next"));
        mvc.perform(get("/admin/accounts").header("Authorization", "Bearer token")
                        .param("query", "cible").param("limit", "2").param("cursor", "opaque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("target@example.com"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].roles[0]").value("LEARNER"))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.items[0].preferences").doesNotExist())
                .andExpect(jsonPath("$.items[0].length()").value(5))
                .andExpect(jsonPath("$.nextCursor").value("next"));
        var actor = ArgumentCaptor.forClass(CurrentAccountDto.class);
        verify(service).search(actor.capture(), eq("cible"), any());
        assertThat(actor.getValue().roles()).containsExactlyInAnyOrder(Role.LEARNER, Role.ADMIN);
    }

    @Test
    void replaceRoles_returnsUpdatedAccountWithoutEtagOrIdempotencyKey() throws Exception {
        authenticate("ADMIN");
        var input = new ReplaceAccountRolesInput(List.of("LEARNER", "AUTHOR"));
        when(service.replaceRoles(any(), eq(targetId), eq(input))).thenReturn(new AdminAccountDto(
                targetId, "target@example.com", "Cible", AccountStatus.ACTIVE, Set.of(Role.LEARNER, Role.AUTHOR)));
        mvc.perform(put("/admin/accounts/{id}/roles", targetId).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"LEARNER\",\"AUTHOR\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(targetId.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"roles\":null}", "{\"roles\":[]}", "{\"roles\":[null]}",
            "{\"roles\":[\"LEARNER\",\"REVIEWER\"]}", "{\"roles\":[\"LEARNER\",\"UNKNOWN\"]}",
            "{\"roles\":[\"LEARNER\",\"LEARNER\"]}"})
    void replaceRoles_invalidRolesReturn422(String json) throws Exception {
        authenticate("ADMIN");
        mvc.perform(put("/admin/accounts/{id}/roles", targetId).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.violations").isNotEmpty());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"roles\":[\"LEARNER\"],\"accountId\":\"other\"}", "{\"roles\":{}}"})
    void replaceRoles_invalidJsonReturns400(String json) throws Exception {
        authenticate("ADMIN");
        mvc.perform(put("/admin/accounts/{id}/roles", targetId).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(service);
    }

    @Test
    void replaceRoles_mapsBusinessErrors() throws Exception {
        authenticate("ADMIN");
        var request = put("/admin/accounts/{id}/roles", targetId).header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"LEARNER\"]}");
        when(service.replaceRoles(any(), eq(targetId), any())).thenThrow(new ForbiddenException("Soi-même."));
        mvc.perform(request).andExpect(status().isForbidden());
        when(service.replaceRoles(any(), eq(targetId), any())).thenThrow(new NotFoundException("Absent."));
        mvc.perform(request).andExpect(status().isNotFound());
        when(service.replaceRoles(any(), eq(targetId), any())).thenThrow(new InvalidStateException("Dernier admin."));
        mvc.perform(request).andExpect(status().isConflict());
    }

    private void authenticate(String... roles) {
        var account = new Account("actor@example.com", "private-hash", "Acteur");
        account.activate();
        ReflectionTestUtils.setField(account, "id", actorId);
        when(accounts.findById(actorId)).thenReturn(Optional.of(account));
        when(decoder.decode("token")).thenReturn(Jwt.withTokenValue("token").header("alg", "RS256")
                .subject(actorId.toString()).claim("roles", List.of(roles)).build());
    }
}
