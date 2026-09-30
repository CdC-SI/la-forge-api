package ch.admin.zas.jweb.laforge.authoring.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.authoring.dto.DraftDto;
import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.authoring.service.AuthoringService;
import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de contrat pour {@link DraftController}. {@code @PreAuthorize} est bien actif dans ce
 * slice car {@code @Import(SecurityConfig.class)} embarque son annotation {@code @EnableMethodSecurity} ;
 * les requêtes authentifiées portent donc l'autorité {@code ROLE_ADMIN} pour satisfaire les
 * contraintes de rôle des routes testées.
 */
@WebMvcTest(controllers = DraftController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class DraftControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthoringService authoringService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountRepository accountRepository;

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticated() {
        when(accountRepository.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Account.class)));
        return jwt().jwt(builder -> builder.subject(ACCOUNT_ID.toString()))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void getDraft_success_exposesETagHeader() throws Exception {
        var draftId = UUID.randomUUID();
        var content = new ExerciseContentInput(
                "Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, List.of(UUID.randomUUID()), 10, List.of(),
                "Prompt", List.of("Objectif"), List.of(),
                new ResponseSpec(ResponseKind.FREE_TEXT, List.of()), List.of(),
                new Correction("Explication", List.of(), List.of(), List.of(), List.of(), List.of()));
        var draft = new DraftDto(
                draftId, UUID.randomUUID(), ACCOUNT_ID, DraftState.DRAFT, 3, 1, content, List.of(), null,
                OffsetDateTime.now());
        when(authoringService.getDraft(any(CurrentAccountDto.class), eq(draftId))).thenReturn(draft);

        mockMvc.perform(get("/authoring/drafts/{id}", draftId).with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""));
    }

    @Test
    void getDraft_notFound_returnsProblem() throws Exception {
        var draftId = UUID.randomUUID();
        when(authoringService.getDraft(any(CurrentAccountDto.class), eq(draftId)))
                .thenThrow(new NotFoundException("Brouillon introuvable."));

        mockMvc.perform(get("/authoring/drafts/{id}", draftId).with(authenticated()))
                .andExpect(status().isNotFound());
    }
}
