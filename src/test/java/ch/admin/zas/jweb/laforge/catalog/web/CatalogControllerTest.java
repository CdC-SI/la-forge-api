package ch.admin.zas.jweb.laforge.catalog.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseDto;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.catalog.dto.TopicDto;
import ch.admin.zas.jweb.laforge.catalog.service.CatalogService;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.OffsetDateTime;
import java.util.List;
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
 * Tests de contrat pour {@link CatalogController}. Toutes les routes exigent un porteur JWT valide
 * (aucun rôle particulier) : {@link SecurityConfig} est importée pour activer la chaîne de
 * sécurité, et {@link JwtDecoder}/{@link AccountRepository} sont mockés car {@code CatalogService}
 * n'utilise pas {@code @CurrentAccount} mais les beans transverses (résolveur d'argument, filtre de
 * sécurité) sont tout de même instanciés dans ce slice. Les filtres de durcissement transverse
 * ({@link MaxRequestBodySizeFilter}, {@link SecurityHeadersFilter}) sont exclus du scan : ils
 * dépendent de {@code LaForgeProperties} (non chargée dans un slice {@code @WebMvcTest}) et sont
 * déjà couverts par leurs propres tests dédiés, pas par les tests de contrôleur.
 */
@WebMvcTest(controllers = CatalogController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountRepository accountRepository;

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
    void listExercises_returnsPageShape() throws Exception {
        var summary = new ExerciseSummaryDto(
                UUID.randomUUID(), 1, "Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, List.of(), 10, List.of(),
                OffsetDateTime.now());
        when(catalogService.listExercises(any(PageQuery.class), any(), any(), any(), any(), any()))
                .thenReturn(Page.last(List.of(summary)));

        mockMvc.perform(get("/exercises").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Titre"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void getLatestExercise_notFound_returnsProblem() throws Exception {
        var exerciseId = UUID.randomUUID();
        when(catalogService.getLatestExercise(exerciseId))
                .thenThrow(new NotFoundException("Aucune version publiée pour cet exercice."));

        mockMvc.perform(get("/exercises/{id}", exerciseId).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.type").value("urn:la-forge:problem:not-found"));
    }

    @Test
    void getExerciseVersion_success() throws Exception {
        var exerciseId = UUID.randomUUID();
        var dto = mockExerciseDto();
        when(catalogService.getExerciseVersion(exerciseId, 2)).thenReturn(dto);

        mockMvc.perform(get("/exercises/{id}/versions/{version}", exerciseId, 2).with(jwt()))
                .andExpect(status().isOk());
    }

    private ExerciseDto mockExerciseDto() {
        return new ExerciseDto(
                UUID.randomUUID(), 2, "Titre", ExerciseType.QUIZ, Difficulty.BEGINNER, List.of(), 10, List.of(),
                OffsetDateTime.now(), "Prompt", List.of(), List.of(),
                new ResponseSpec(ResponseKind.FREE_TEXT, List.of()), 0);
    }
}
