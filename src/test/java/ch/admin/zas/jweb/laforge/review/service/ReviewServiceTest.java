package ch.admin.zas.jweb.laforge.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.review.repository.ReviewItemRepository;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests unitaires légers de {@link ReviewService}. La construction fine de la {@link
 * Specification} par état n'est pas vérifiable sans dépôt réel (H2) ; ces tests se limitent donc
 * à confirmer que le filtre par défaut ({@code DUE}) ne lève pas d'exception et que la forme de
 * page retournée est cohérente lorsque le dépôt ne renvoie aucune ligne.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewItemRepository reviewItemRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);

    private ReviewService newService() {
        return new ReviewService(reviewItemRepository, clock);
    }

    private static Account newLearner() {
        var account = new Account("learner@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    @Test
    void listMyReviews_stateAbsent_utiliseDueParDefautSansException() {
        when(reviewItemRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var page = newService().listMyReviews(CurrentAccountDto.from(newLearner()), new PageQuery(20, null), null);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void listMyReviews_pageVide_neProduitPasDeCurseurSuivant() {
        when(reviewItemRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        var page = newService().listMyReviews(
                CurrentAccountDto.from(newLearner()),
                new PageQuery(20, null),
                ch.admin.zas.jweb.laforge.review.domain.ReviewState.SCHEDULED);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }
}
