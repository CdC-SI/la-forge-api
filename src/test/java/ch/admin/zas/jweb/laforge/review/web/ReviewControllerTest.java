package ch.admin.zas.jweb.laforge.review.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.web.MaxRequestBodySizeFilter;
import ch.admin.zas.jweb.laforge.common.web.SecurityHeadersFilter;
import ch.admin.zas.jweb.laforge.review.service.ReviewService;
import ch.admin.zas.jweb.laforge.security.config.SecurityConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
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

/** Tests de contrat pour {@link ReviewController}. */
@WebMvcTest(controllers = ReviewController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {MaxRequestBodySizeFilter.class, SecurityHeadersFilter.class}))
@Import(SecurityConfig.class)
class ReviewControllerTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void listMyReviews_success_returnsEmptyPage() throws Exception {
        when(accountRepository.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(Account.class)));
        when(reviewService.listMyReviews(any(CurrentAccountDto.class), any(), any())).thenReturn(Page.last(List.of()));

        mockMvc.perform(get("/me/reviews").with(jwt().jwt(builder -> builder.subject(ACCOUNT_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void listMyReviews_withoutJwt_isUnauthenticated() throws Exception {
        mockMvc.perform(get("/me/reviews")).andExpect(status().isUnauthorized());
    }
}
