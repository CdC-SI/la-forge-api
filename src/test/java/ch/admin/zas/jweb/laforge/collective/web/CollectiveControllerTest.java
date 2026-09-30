package ch.admin.zas.jweb.laforge.collective.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeState;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeCreatedDto;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeDto;
import ch.admin.zas.jweb.laforge.collective.service.CollectiveService;
import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
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

/** Tests de contrat pour {@link CollectiveController} : création de défis et adhésion. */
@WebMvcTest(controllers = CollectiveController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class CollectiveControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectiveService collectiveService;

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
    void createChallenge_success_returns201() throws Exception {
        var exerciseSummary = new ExerciseSummaryDto(
                UUID.randomUUID(), 1, "Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, List.of(), 10, List.of(),
                OffsetDateTime.now());
        var challenge = new ChallengeDto(
                UUID.randomUUID(), "Défi", exerciseSummary, ACCOUNT_ID, OffsetDateTime.now().plusDays(1),
                ChallengeState.OPEN, 1, false);
        when(collectiveService.createChallenge(any(CurrentAccountDto.class), any()))
                .thenReturn(new ChallengeCreatedDto(challenge, "abcdefghijklmnopqrstuvwxyzabcdef"));

        var body = """
                {"title":"Défi","exerciseId":"%s","exerciseVersion":1,"closesAt":"%s"}
                """.formatted(UUID.randomUUID(), OffsetDateTime.now().plusDays(2));

        mockMvc.perform(post("/challenges")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.challenge.title").value("Défi"))
                .andExpect(jsonPath("$.joinCode").exists());
    }

    @Test
    void createChallenge_pastClosingDate_isUnprocessable() throws Exception {
        var body = """
                {"title":"Défi","exerciseId":"%s","exerciseVersion":1,"closesAt":"2000-01-01T00:00:00Z"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/challenges")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
