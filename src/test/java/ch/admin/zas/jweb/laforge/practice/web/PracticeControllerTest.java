package ch.admin.zas.jweb.laforge.practice.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.dto.AttemptDto;
import ch.admin.zas.jweb.laforge.practice.domain.FreeTextAnswer;
import ch.admin.zas.jweb.laforge.practice.service.PracticeService;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

/** Tests de contrat pour {@link PracticeController} : création, soumission et abandon de tentatives. */
@WebMvcTest(controllers = PracticeController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class PracticeControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PracticeService practiceService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountRepository accountRepository;

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticated() {
        when(accountRepository.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Account.class)));
        return jwt().jwt(builder -> builder.subject(ACCOUNT_ID.toString()));
    }

    @Test
    void createAttempt_success_returns201() throws Exception {
        var exerciseId = UUID.randomUUID();
        var dto = new AttemptDto(
                UUID.randomUUID(), exerciseId, 1, null, null, AttemptStatus.IN_PROGRESS,
                OffsetDateTime.now(), null, null, List.of(), false);
        when(practiceService.createAttempt(any(Account.class), any())).thenReturn(dto);

        var body = """
                {"exerciseId":"%s","exerciseVersion":1}
                """.formatted(exerciseId);

        mockMvc.perform(post("/attempts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void createAttempt_invalidBody_isUnprocessable() throws Exception {
        var body = """
                {"exerciseVersion":0}
                """;

        mockMvc.perform(post("/attempts")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void submitAttempt_notFound_returnsProblem() throws Exception {
        var attemptId = UUID.randomUUID();
        when(practiceService.submitAttempt(any(Account.class), eq(attemptId), any()))
                .thenThrow(new NotFoundException("Tentative introuvable."));

        var body = """
                {"answer":{"kind":"FREE_TEXT","text":"réponse","reasoning":"car...","confidence":3}}
                """;

        mockMvc.perform(put("/attempts/{id}/submission", attemptId)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
