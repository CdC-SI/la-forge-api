package ch.admin.zas.jweb.laforge.catalog.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicDto;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicInput;
import ch.admin.zas.jweb.laforge.catalog.service.CatalogService;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests de contrat pour {@link CatalogController}. Toutes les routes exigent un porteur JWT valide
 * (aucun rôle particulier) : {@link SecurityConfig} est importée pour activer la chaîne de
 * sécurité, et {@link JwtDecoder}/{@link AccountRepository} sont mockés pour résoudre le compte
 * courant à partir du JWT. Les filtres de durcissement transverse
 * ({@link MaxRequestBodySizeFilter}, {@link SecurityHeadersFilter}) sont exclus du scan : ils
 * dépendent de {@code LaForgeProperties} (non chargée dans un slice {@code @WebMvcTest}) et sont
 * déjà couverts par leurs propres tests dédiés, pas par les tests de contrôleur.
 */
@WebMvcTest(controllers = CatalogController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class CatalogControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountRepository accountRepository;

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticated() {
        var account = new Account("catalog@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        return jwt().jwt(builder -> builder.subject(ACCOUNT_ID.toString()));
    }

    @Test
    void listTopics_returnsPageShape() throws Exception {
        var topic = new TopicDto(UUID.randomUUID(), "java-streams", "Flux Java");
        when(catalogService.listTopics(any(PageQuery.class))).thenReturn(Page.of(List.of(topic), "next-cursor"));

        mockMvc.perform(get("/topics").param("limit", "10").param("cursor", "abc").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].slug").value("java-streams"))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"));

        verify(catalogService).listTopics(eq(new PageQuery(10, "abc")));
    }

    @Test
    void listTopics_withoutJwt_isUnauthenticated() throws Exception {
        mockMvc.perform(get("/topics"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createTopic_asAuthor_returns201() throws Exception {
        var id = UUID.randomUUID();
        when(catalogService.createTopic(any(TopicInput.class))).thenReturn(new TopicDto(id, "java-records", "Records Java"));

        mockMvc.perform(post("/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUTHOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"java-records","label":"Records Java"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.slug").value("java-records"));

        verify(catalogService).createTopic(new TopicInput("java-records", "Records Java"));
    }

    @Test
    void createTopic_withId_isRejectedAsUnknownProperty() throws Exception {
        mockMvc.perform(post("/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"%s","slug":"java","label":"Java"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().is4xxClientError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createTopic_asLearner_isForbidden() throws Exception {
        mockMvc.perform(post("/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LEARNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"java","label":"Java"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createTopic_invalidSlug_isUnprocessable() throws Exception {
        mockMvc.perform(post("/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUTHOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"Java Records","label":"Records"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createTopic_conflict_returns409() throws Exception {
        when(catalogService.createTopic(any(TopicInput.class)))
                .thenThrow(new InvalidStateException("Un thème existe déjà avec ce slug."));

        mockMvc.perform(post("/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUTHOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"java","label":"Java"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void listExercises_returnsPageShape() throws Exception {
        var summary = new ExerciseSummaryDto(
                UUID.randomUUID(), 1, "Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, List.of(), 10, List.of(),
                OffsetDateTime.now(), true);
        when(catalogService.listExercises(any(CurrentAccountDto.class), any(PageQuery.class), any(), any(), any(), any(), any()))
                .thenReturn(Page.last(List.of(summary)));

        mockMvc.perform(get("/exercises").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Titre"))
                .andExpect(jsonPath("$.items[0].completed").value(true))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
        verify(catalogService).listExercises(argThat(account -> account.id().equals(ACCOUNT_ID)),
                any(PageQuery.class), any(), any(), any(), any(), any());
    }

    @Test
    void getLatestExercise_notFound_returnsProblem() throws Exception {
        var exerciseId = UUID.randomUUID();
        when(catalogService.getLatestExercise(any(CurrentAccountDto.class), eq(exerciseId)))
                .thenThrow(new NotFoundException("Aucune version publiée pour cet exercice."));

        mockMvc.perform(get("/exercises/{id}", exerciseId).with(authenticated()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.type").value("urn:la-forge:problem:not-found"));
    }

    @Test
    void getExerciseVersion_success() throws Exception {
        var exerciseId = UUID.randomUUID();
        var dto = mockExerciseDto();
        when(catalogService.getExerciseVersion(any(CurrentAccountDto.class), eq(exerciseId), eq(2))).thenReturn(dto);

        mockMvc.perform(get("/exercises/{id}/versions/{version}", exerciseId, 2).with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.correction").doesNotExist());
        verify(catalogService).getExerciseVersion(
                argThat(account -> account.id().equals(ACCOUNT_ID)), eq(exerciseId), eq(2));
    }

    @Test
    void getLatestExercise_transmetLeCompteEtRetourneCompletedObligatoire() throws Exception {
        var dto = mockExerciseDto();
        when(catalogService.getLatestExercise(any(CurrentAccountDto.class), eq(dto.id()))).thenReturn(dto);

        mockMvc.perform(get("/exercises/{id}", dto.id()).with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.correction").doesNotExist());

        verify(catalogService).getLatestExercise(argThat(account -> account.id().equals(ACCOUNT_ID)), eq(dto.id()));
    }

    @Test
    void exercises_sansJwt_exigentUneAuthentification() throws Exception {
        mockMvc.perform(get("/exercises")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/exercises/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/exercises/{id}/versions/1", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    private ExerciseDto mockExerciseDto() {
        return new ExerciseDto(
                UUID.randomUUID(), 2, "Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, List.of(), 10, List.of(),
                OffsetDateTime.now(), "Prompt", List.of(), List.of(),
                new ResponseSpec(ResponseKind.FREE_TEXT, List.of()), 0, false);
    }
}
