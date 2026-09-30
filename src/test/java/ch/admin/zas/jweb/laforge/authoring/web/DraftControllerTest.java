package ch.admin.zas.jweb.laforge.authoring.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
import ch.admin.zas.jweb.laforge.common.error.StaleVersionException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.error.Violation;
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
import org.springframework.http.MediaType;
import org.mockito.ArgumentCaptor;

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
                draftId, UUID.randomUUID(), ACCOUNT_ID, DraftState.DRAFT, 3, 1, content, null,
                OffsetDateTime.now());
        when(authoringService.getDraft(any(CurrentAccountDto.class), eq(draftId))).thenReturn(draft);

        mockMvc.perform(get("/authoring/drafts/{id}", draftId).with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""));
    }

    @Test
    void createDraft_titleOnlyNormalizesCollections() throws Exception {
        when(authoringService.createDraft(any(), eq(null), any())).thenReturn(minimalDraft());

        mockMvc.perform(post("/authoring/drafts").with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":{"title":"Un brouillon"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.reviews").doesNotExist());

        var content = ArgumentCaptor.forClass(ExerciseContentInput.class);
        verify(authoringService).createDraft(any(), eq(null), content.capture());
        org.assertj.core.api.Assertions.assertThat(content.getValue().topicIds()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(content.getValue().estimatedMinutes()).isNull();
    }

    @Test
    void replaceDraft_acceptsTechnologyNameOnly() throws Exception {
        var draft = minimalDraft();
        when(authoringService.replaceDraft(any(), eq(draft.id()), eq(0), any())).thenReturn(draft);
        mockMvc.perform(put("/authoring/drafts/{id}", draft.id()).with(authenticated())
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":{"title":"Titre","technologies":[{"technology":"Java"}]}}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void publishDraft_missingPreconditionIsRejected() throws Exception {
        mockMvc.perform(post("/authoring/drafts/{id}/publish", UUID.randomUUID()).with(authenticated()))
                .andExpect(status().isPreconditionRequired());
        verifyNoInteractions(authoringService);
    }

    @Test
    void publishDraft_staleRevisionIsRejected() throws Exception {
        when(authoringService.publishDraft(any(), any(), eq(0)))
                .thenThrow(new StaleVersionException("Révision obsolète."));
        mockMvc.perform(post("/authoring/drafts/{id}/publish", UUID.randomUUID()).with(authenticated())
                        .header("If-Match", "\"0\""))
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void publishDraft_incompleteContentReportsFields() throws Exception {
        when(authoringService.publishDraft(any(), any(), eq(0)))
                .thenThrow(new ValidationFailedException("Contenu incomplet.",
                        List.of(new Violation("content.correction", "Le corrigé est requis."))));
        mockMvc.perform(post("/authoring/drafts/{id}/publish", UUID.randomUUID()).with(authenticated())
                        .header("If-Match", "\"0\""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0].field").value("content.correction"));
    }

    @Test
    void learnerCannotCreateDraft() throws Exception {
        var learner = authenticated();
        mockMvc.perform(post("/authoring/drafts").with(learner)
                        .with(jwt().jwt(builder -> builder.subject(ACCOUNT_ID.toString()))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_LEARNER")))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"content":{"title":"Titre"}}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(authoringService);
    }

    @Test
    void anonymousCannotReadDrafts() throws Exception {
        mockMvc.perform(get("/authoring/drafts")).andExpect(status().isUnauthorized());
    }

    @Test
    void createDraft_unknownFieldIsRejected() throws Exception {
        mockMvc.perform(post("/authoring/drafts").with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"content":{"title":"Titre","unknown":true}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editorialReviewRoutesAreRemoved() throws Exception {
        for (var action : List.of("submit", "review")) {
            mockMvc.perform(post("/authoring/drafts/{id}/{action}", UUID.randomUUID(), action)
                            .with(authenticated()).header("If-Match", "\"0\""))
                    .andExpect(status().isNotFound());
        }
        verifyNoInteractions(authoringService);
    }

    @Test
    void removedDraftStatesReturnBadRequest() throws Exception {
        for (var state : List.of("IN_REVIEW", "APPROVED")) {
            mockMvc.perform(get("/authoring/drafts").with(authenticated()).param("state", state))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("state"));
        }
        verifyNoInteractions(authoringService);
    }

    private DraftDto minimalDraft() {
        return new DraftDto(UUID.randomUUID(), UUID.randomUUID(), ACCOUNT_ID, DraftState.DRAFT, 0, 0,
                new ExerciseContentInput("Un brouillon", null, null, null, null, null, null, null, null, null, null, null),
                null, OffsetDateTime.now());
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
