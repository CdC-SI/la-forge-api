package ch.admin.zas.jweb.laforge.collective.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeParticipant;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeState;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import java.time.OffsetDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link ChallengeRepository} : round-trip et la requête du tableau de
 * bord ({@code findOpenChallengesForAccount}) couvrant à la fois créateur et participant.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class ChallengeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeParticipantRepository challengeParticipantRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    private ExerciseVersion persistExerciseVersion() {
        var exercise = entityManager.persistAndFlush(new Exercise());
        return entityManager.persistAndFlush(ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of()));
    }

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var creator = persistAccount("creator@example.com");
        var version = persistExerciseVersion();
        var closesAt = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        var challenge = new Challenge("Défi Java", version, creator, closesAt, "join-code-hash");

        var saved = challengeRepository.saveAndFlush(challenge);
        entityManager.clear();

        var reloaded = challengeRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Défi Java");
        assertThat(reloaded.getExerciseVersion().getId()).isEqualTo(version.getId());
        assertThat(reloaded.getCreator().getId()).isEqualTo(creator.getId());
        assertThat(reloaded.getClosesAt()).isEqualTo(closesAt);
        assertThat(reloaded.getJoinCodeHash()).isEqualTo("join-code-hash");
        assertThat(reloaded.getState()).isEqualTo(ChallengeState.OPEN);
    }

    @Test
    void findByJoinCodeHash_returnsMatchingChallenge() {
        var creator = persistAccount("hash-lookup@example.com");
        var version = persistExerciseVersion();
        challengeRepository.saveAndFlush(
                new Challenge("Défi", version, creator, OffsetDateTime.parse("2030-01-01T00:00:00Z"), "unique-hash"));

        assertThat(challengeRepository.findByJoinCodeHash("unique-hash")).isPresent();
        assertThat(challengeRepository.findByJoinCodeHash("absent")).isEmpty();
    }

    @Test
    void findOpenChallengesForAccount_includesCreatedAndJoinedOpenChallenges() {
        var creator = persistAccount("creator2@example.com");
        var participant = persistAccount("participant@example.com");
        var otherCreator = persistAccount("other-creator@example.com");
        var stranger = persistAccount("stranger@example.com");
        var version = persistExerciseVersion();

        var createdByCreator = challengeRepository.saveAndFlush(
                new Challenge("Créé", version, creator, OffsetDateTime.parse("2030-01-01T00:00:00Z"), "h1"));
        var joinedByParticipant = challengeRepository.saveAndFlush(
                new Challenge("Rejoint", version, otherCreator, OffsetDateTime.parse("2030-02-01T00:00:00Z"), "h2"));
        challengeParticipantRepository.saveAndFlush(
                new ChallengeParticipant(joinedByParticipant, participant, OffsetDateTime.parse("2024-01-01T00:00:00Z")));
        var closedChallenge = challengeRepository.saveAndFlush(
                new Challenge("Fermé", version, creator, OffsetDateTime.parse("2030-03-01T00:00:00Z"), "h3"));
        closedChallenge.close();
        challengeRepository.saveAndFlush(closedChallenge);
        entityManager.clear();

        var openForCreator = challengeRepository.findOpenChallengesForAccount(creator.getId());
        assertThat(openForCreator).extracting(Challenge::getId).containsExactly(createdByCreator.getId());

        var openForParticipant = challengeRepository.findOpenChallengesForAccount(participant.getId());
        assertThat(openForParticipant).extracting(Challenge::getId).containsExactly(joinedByParticipant.getId());

        assertThat(challengeRepository.findOpenChallengesForAccount(stranger.getId())).isEmpty();
    }
}
