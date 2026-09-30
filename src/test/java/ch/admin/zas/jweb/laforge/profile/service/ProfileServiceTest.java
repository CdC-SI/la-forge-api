package ch.admin.zas.jweb.laforge.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.catalog.service.CatalogService;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeParticipantRepository;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeRepository;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.discovery.service.DiscoveryService;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import ch.admin.zas.jweb.laforge.profile.repository.PreferencesRepository;
import ch.admin.zas.jweb.laforge.review.repository.ReviewItemRepository;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private PreferencesRepository preferencesRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private DiscoveryService discoveryService;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private ChallengeParticipantRepository challengeParticipantRepository;
    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private ReviewItemRepository reviewItemRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ExerciseCompletionService exerciseCompletionService;

    @Test
    void getDashboard_transmetLeCompteEtCompleteLesDefisEnUnLot() {
        var account = new Account("dashboard@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        var current = CurrentAccountDto.from(account);
        var exercise = new Exercise();
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        var version = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 2, Set.of());
        var first = new Challenge("Premier défi", version, account, OffsetDateTime.now().plusDays(1), "hash1");
        var second = new Challenge("Second défi", version, account, OffsetDateTime.now().plusDays(2), "hash2");
        ReflectionTestUtils.setField(first, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(second, "id", UUID.randomUUID());
        when(catalogService.listExercises(eq(current), any(PageQuery.class), any(), any(), any(), any(), any()))
                .thenReturn(Page.last(List.of(ExerciseSummaryDto.from(version, true))));
        when(discoveryService.listArticles(any(PageQuery.class), any(), any(), any())).thenReturn(Page.last(List.of()));
        when(challengeRepository.findOpenChallengesForAccount(account.getId())).thenReturn(List.of(first, second));
        var ids = List.of(exercise.getId(), exercise.getId());
        when(exerciseCompletionService.completedExerciseIds(account.getId(), ids)).thenReturn(Set.of(exercise.getId()));
        var service = new ProfileService(preferencesRepository, topicRepository, catalogService, discoveryService,
                challengeRepository, challengeParticipantRepository, attemptRepository, reviewItemRepository,
                accountRepository, exerciseCompletionService, Clock.systemUTC());

        var dashboard = service.getDashboard(current);

        assertThat(dashboard.recommendedExercises()).allMatch(ExerciseSummaryDto::completed);
        assertThat(dashboard.openChallenges()).hasSize(2).allMatch(challenge -> challenge.exercise().completed());
        assertThat(dashboard.openChallenges()).allMatch(challenge -> !challenge.responsesUnlocked());
        verify(exerciseCompletionService).completedExerciseIds(account.getId(), ids);
        verify(catalogService).listExercises(eq(current), eq(new PageQuery(3, null)), any(), any(), any(), any(), any());
    }
}
