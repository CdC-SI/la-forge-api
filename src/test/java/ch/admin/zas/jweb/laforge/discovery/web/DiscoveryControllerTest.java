package ch.admin.zas.jweb.laforge.discovery.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleSummaryDto;
import ch.admin.zas.jweb.laforge.discovery.service.DiscoveryService;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Tests de contrat pour {@link DiscoveryController}. La lecture ({@code GET /articles}) exige un
 * porteur JWT (aucun rôle particulier). L'édition requiert AUTHOR ou ADMIN.
 */
@WebMvcTest(controllers = DiscoveryController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscoveryService discoveryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountRepository accountRepository;

    private static final String ARTICLE = """
            {"title":"Titre","summary":"Résumé","topicIds":[],"technologies":[{"technology":"Java"}],
             "bodyMarkdown":"Texte","sources":[{"title":"Source","url":"https://example.com",
             "accessedAt":"2026-01-01T00:00:00Z"}],"relatedExercises":[]}
            """;

    @ParameterizedTest
    @ValueSource(strings = {"AUTHOR", "ADMIN"})
    void editing_isAllowedForAuthorOrAdmin(String role) throws Exception {
        var authentication = authenticated(role);
        mockMvc.perform(post("/authoring/articles").with(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(ARTICLE))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/authoring/articles/{id}", UUID.randomUUID()).with(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedRevision\":1,\"content\":" + ARTICLE + "}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEARNER", "REVIEWER"})
    void editing_rejectsLearnerAndRetiredReviewer(String role) throws Exception {
        var authentication = authenticated(role);
        mockMvc.perform(post("/authoring/articles").with(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(ARTICLE))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/authoring/articles/{id}", UUID.randomUUID()).with(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedRevision\":1,\"content\":" + ARTICLE + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listArticles_success() throws Exception {
        var summary = new ArticleSummaryDto(
                UUID.randomUUID(), "Titre", "Résumé", List.of(), List.of(), OffsetDateTime.now());
        when(discoveryService.listArticles(any(), any(), any(), any())).thenReturn(Page.last(List.of(summary)));

        mockMvc.perform(get("/articles").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].title").value("Titre"));
    }

    @Test
    void getArticle_notFound_returnsProblem() throws Exception {
        var articleId = UUID.randomUUID();
        when(discoveryService.getArticle(any(), eq(articleId))).thenThrow(new NotFoundException("Fiche introuvable."));

        mockMvc.perform(get("/articles/{id}", articleId).with(authenticated("LEARNER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:la-forge:problem:not-found"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(String role) {
        var account = new ch.admin.zas.jweb.laforge.security.domain.Account("actor@example.com", "hash", "Acteur");
        account.activate();
        var id = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", id);
        when(accountRepository.findById(id)).thenReturn(java.util.Optional.of(account));
        return jwt().jwt(builder -> builder.subject(id.toString())).authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }
}
