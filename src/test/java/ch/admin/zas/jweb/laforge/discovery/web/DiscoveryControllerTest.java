package ch.admin.zas.jweb.laforge.discovery.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * porteur JWT (aucun rôle particulier). La publication éditoriale, protégée par
 * {@code @PreAuthorize}, n'est pas couverte ici (dépasse le périmètre minimal retenu pour ce
 * contrôleur ; voir {@link ch.admin.zas.jweb.laforge.authoring.web.DraftControllerTest} pour un
 * exemple de test de route protégée par rôle).
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
        when(discoveryService.getArticle(eq(articleId))).thenThrow(new NotFoundException("Fiche introuvable."));

        mockMvc.perform(get("/articles/{id}", articleId).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:la-forge:problem:not-found"));
    }
}
