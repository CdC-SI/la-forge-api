package ch.admin.zas.jweb.laforge.tutor.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.tutor.dto.TutorExchangeDto;
import ch.admin.zas.jweb.laforge.tutor.service.TutorService;
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

/** Tests de contrat pour {@link TutorController}. */
@WebMvcTest(controllers = TutorController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class TutorControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TutorService tutorService;

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
    void askTutor_success_returns201() throws Exception {
        var attemptId = UUID.randomUUID();
        var dto = new TutorExchangeDto(
                UUID.randomUUID(), "Pourquoi ?", "Réponse.", List.of(), OffsetDateTime.now(), false);
        when(tutorService.askTutor(any(CurrentAccountDto.class), eq(attemptId), eq("Pourquoi ?"))).thenReturn(dto);

        var body = """
                {"question":"Pourquoi ?"}
                """;

        mockMvc.perform(post("/attempts/{id}/tutor-exchanges", attemptId)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.question").value("Pourquoi ?"));
    }

    @Test
    void askTutor_blankQuestion_isUnprocessable() throws Exception {
        var attemptId = UUID.randomUUID();
        var body = """
                {"question":""}
                """;

        mockMvc.perform(post("/attempts/{id}/tutor-exchanges", attemptId)
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
