package ch.admin.zas.jweb.laforge.collective.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeParticipantRepository;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeRepository;
import ch.admin.zas.jweb.laforge.collective.repository.DiscussionCommentRepository;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.common.security.SecureTokenFactory;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CollectiveServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private ChallengeParticipantRepository challengeParticipantRepository;
    @Mock
    private ExerciseVersionRepository exerciseVersionRepository;
    @Mock
    private AttemptRepository attemptRepository;
    @Mock
    private ExerciseCompletionService exerciseCompletionService;
    @Mock
    private DiscussionCommentRepository discussionCommentRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SecureTokenFactory secureTokenFactory;

    @Test
    void listMyChallenges_completeLaPageEnUnLotSansDebloquerLesReponses() {
        var account = new Account("participant@example.com", "hash", "Apprenant");
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        var first = challenge(account);
        var second = challenge(account);
        var next = challenge(account);
        when(challengeRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second, next)));
        var ids = List.of(first.getExerciseVersion().getExercise().getId(), second.getExerciseVersion().getExercise().getId());
        when(exerciseCompletionService.completedExerciseIds(account.getId(), ids)).thenReturn(Set.of(ids.getFirst()));
        var service = new CollectiveService(challengeRepository, challengeParticipantRepository, exerciseVersionRepository,
                attemptRepository, exerciseCompletionService, discussionCommentRepository, accountRepository,
                secureTokenFactory, Clock.systemUTC());

        var page = service.listMyChallenges(CurrentAccountDto.from(account), new PageQuery(2, null));

        assertThat(page.items()).extracting(item -> item.exercise().completed()).containsExactly(true, false);
        assertThat(page.items()).allMatch(item -> !item.responsesUnlocked());
        assertThat(page.nextCursor()).isNotBlank();
        verify(exerciseCompletionService).completedExerciseIds(account.getId(), ids);
    }

    private static Challenge challenge(Account creator) {
        var exercise = new Exercise();
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        var version = ExerciseVersionFixtures.sampleExerciseVersion(exercise, 2, Set.of());
        var challenge = new Challenge("Défi", version, creator, OffsetDateTime.parse("2026-12-01T00:00:00Z"), "hash");
        ReflectionTestUtils.setField(challenge, "id", UUID.randomUUID());
        return challenge;
    }
}
