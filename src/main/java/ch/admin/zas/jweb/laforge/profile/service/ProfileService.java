package ch.admin.zas.jweb.laforge.profile.service;

import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.catalog.service.CatalogService;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeDto;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeParticipantRepository;
import ch.admin.zas.jweb.laforge.collective.repository.ChallengeRepository;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.discovery.service.DiscoveryService;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.repository.TopicProgressProjection;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import ch.admin.zas.jweb.laforge.profile.domain.Preferences;
import ch.admin.zas.jweb.laforge.profile.dto.DashboardDto;
import ch.admin.zas.jweb.laforge.profile.dto.PreferencesDto;
import ch.admin.zas.jweb.laforge.profile.dto.ProgressDto;
import ch.admin.zas.jweb.laforge.profile.dto.TopicProgressDto;
import ch.admin.zas.jweb.laforge.profile.dto.UserDto;
import ch.admin.zas.jweb.laforge.profile.repository.PreferencesRepository;
import ch.admin.zas.jweb.laforge.review.repository.ReviewItemRepository;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profil de l'apprenant : préférences, tableau de bord de suggestions et progression privée
 * (tag {@code Profile}). Les préférences sont créées paresseusement à la première écriture.
 */
@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final PreferencesRepository preferencesRepository;
    private final TopicRepository topicRepository;
    private final CatalogService catalogService;
    private final DiscoveryService discoveryService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final AttemptRepository attemptRepository;
    private final ReviewItemRepository reviewItemRepository;
    private final AccountRepository accountRepository;
    private final Clock clock;
    private final ExerciseCompletionService exerciseCompletionService;

    public ProfileService(
            PreferencesRepository preferencesRepository,
            TopicRepository topicRepository,
            CatalogService catalogService,
            DiscoveryService discoveryService,
            ChallengeRepository challengeRepository,
            ChallengeParticipantRepository challengeParticipantRepository,
            AttemptRepository attemptRepository,
            ReviewItemRepository reviewItemRepository,
            AccountRepository accountRepository,
            ExerciseCompletionService exerciseCompletionService,
            Clock clock) {
        this.preferencesRepository = preferencesRepository;
        this.topicRepository = topicRepository;
        this.catalogService = catalogService;
        this.discoveryService = discoveryService;
        this.challengeRepository = challengeRepository;
        this.challengeParticipantRepository = challengeParticipantRepository;
        this.attemptRepository = attemptRepository;
        this.reviewItemRepository = reviewItemRepository;
        this.accountRepository = accountRepository;
        this.exerciseCompletionService = exerciseCompletionService;
        this.clock = clock;
    }

    public UserDto getMe(CurrentAccountDto account) {
        return new UserDto(account.id(), account.displayName(), account.roles(), currentPreferences(account));
    }

    public PreferencesDto currentPreferences(CurrentAccountDto account) {
        return preferencesRepository.findByAccount_Id(account.id())
                .map(PreferencesDto::from)
                .orElseGet(PreferencesDto::defaultPreferences);
    }

    /**
     * Remplace intégralement les préférences, en créant la ligne au besoin.
     *
     * @throws NotFoundException si un des {@code topicIds} n'existe pas
     */
    @Transactional
    public PreferencesDto replacePreferences(CurrentAccountDto account, PreferencesDto input) {
        var topics = resolveTopics(input.topicIds());
        var preferences = preferencesRepository.findByAccount_Id(account.id())
                .orElseGet(() -> new Preferences(accountRepository.getReferenceById(account.id())));
        preferences.replaceWith(input.stack(), topics, input.difficulty(), input.sessionMinutes(), input.locale(), input.timeZone());
        return PreferencesDto.from(preferencesRepository.save(preferences));
    }

    /**
     * Suggestions personnelles : trois derniers exercices et fiches correspondant au premier thème
     * préféré (ou aux plus récents en l'absence de préférence), défis ouverts liés au compte, et
     * disponibilité du tuteur IA (toujours désactivé en v1, voir {@code tutor-feature}).
     */
    public DashboardDto getDashboard(CurrentAccountDto account) {
        var preferences = currentPreferences(account);
        var preferredTopicId = preferences.topicIds().isEmpty() ? null : preferences.topicIds().get(0);

        var recommendedExercises = catalogService
                .listExercises(account, new PageQuery(3, null), null, preferences.difficulty(), preferredTopicId, null, null)
                .items();
        var discoveries = discoveryService.listArticles(new PageQuery(3, null), preferredTopicId, null, null).items();
        var challenges = challengeRepository.findOpenChallengesForAccount(account.id()).stream()
                .limit(10)
                .toList();
        var completed = exerciseCompletionService.completedExerciseIds(account.id(),
                challenges.stream().map(challenge -> challenge.getExerciseVersion().getExercise().getId()).toList());
        var openChallenges = challenges.stream()
                .map(challenge -> ChallengeDto.from(
                        challenge,
                        (int) challengeParticipantRepository.countByChallenge_Id(challenge.getId()),
                        attemptRepository
                                .findByChallengeIdAndLearner_Id(challenge.getId(), account.id())
                                .filter(attempt -> attempt.getStatus() == AttemptStatus.SUBMITTED)
                                .isPresent(),
                        completed.contains(challenge.getExerciseVersion().getExercise().getId())))
                .toList();
        var dueReviewCount = (int) reviewItemRepository.countByLearner_IdAndCompletedAtIsNullAndDueAtLessThanEqual(
                account.id(), OffsetDateTime.now(clock));

        return new DashboardDto(recommendedExercises, discoveries, dueReviewCount, openChallenges, false);
    }

    /** Progression personnelle, limitée aux réponses à choix corrigées automatiquement. */
    public ProgressDto getProgress(CurrentAccountDto account) {
        var submittedAttempts = (int) attemptRepository.countByLearner_IdAndStatus(account.id(), AttemptStatus.SUBMITTED);
        var since = OffsetDateTime.now(clock).minusDays(30);
        var activeDays = attemptRepository.findSubmittedAtSince(account.id(), since).stream()
                .map(instant -> instant.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate())
                .collect(Collectors.toSet())
                .size();
        var dueReviewCount = (int) reviewItemRepository.countByLearner_IdAndCompletedAtIsNullAndDueAtLessThanEqual(
                account.id(), OffsetDateTime.now(clock));
        List<TopicProgressDto> topics = attemptRepository.findTopicProgress(account.id()).stream()
                .map(TopicProgressDto::from)
                .toList();
        return new ProgressDto(submittedAttempts, activeDays, dueReviewCount, topics);
    }

    private LinkedHashSet<Topic> resolveTopics(List<java.util.UUID> topicIds) {
        var topics = new LinkedHashSet<Topic>();
        for (var topicId : topicIds) {
            topics.add(topicRepository.findById(topicId)
                    .orElseThrow(() -> new NotFoundException("Thème introuvable : " + topicId)));
        }
        return topics;
    }
}
