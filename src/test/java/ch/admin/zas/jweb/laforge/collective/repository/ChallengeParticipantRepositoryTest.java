package ch.admin.zas.jweb.laforge.collective.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeParticipant;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link ChallengeParticipantRepository} : round-trip, unicité de
 * l'inscription (couple défi/compte) et requêtes dérivées.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class ChallengeParticipantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChallengeParticipantRepository challengeParticipantRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    private Challenge persistChallenge(Account creator) {
        var exercise = entityManager.persistAndFlush(new Exercise());
        ExerciseVersion version = entityManager.persistAndFlush(ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of()));
        return entityManager.persistAndFlush(
                new Challenge("Défi", version, creator, OffsetDateTime.parse("2030-01-01T00:00:00Z"), "hash"));
    }

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var creator = persistAccount("creator@example.com");
        var challenge = persistChallenge(creator);
        var joinedAt = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        var participant = new ChallengeParticipant(challenge, creator, joinedAt);

        var saved = challengeParticipantRepository.saveAndFlush(participant);
        entityManager.clear();

        var reloaded = challengeParticipantRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getChallenge().getId()).isEqualTo(challenge.getId());
        assertThat(reloaded.getAccount().getId()).isEqualTo(creator.getId());
        assertThat(reloaded.getJoinedAt()).isEqualTo(joinedAt);
    }

    @Test
    void findByChallenge_IdAndAccount_Id_returnsMatchingParticipant() {
        var creator = persistAccount("owner@example.com");
        var challenge = persistChallenge(creator);
        challengeParticipantRepository.saveAndFlush(
                new ChallengeParticipant(challenge, creator, OffsetDateTime.parse("2024-01-01T00:00:00Z")));

        assertThat(challengeParticipantRepository.findByChallenge_IdAndAccount_Id(challenge.getId(), creator.getId())).isPresent();
    }

    @Test
    void findByAccount_IdOrderByJoinedAtDesc_ordersMostRecentFirst() {
        var creator = persistAccount("multi@example.com");
        var challenge1 = persistChallenge(creator);
        var challenge2 = persistChallenge(creator);
        var older = new ChallengeParticipant(challenge1, creator, OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        var newer = new ChallengeParticipant(challenge2, creator, OffsetDateTime.parse("2024-02-01T00:00:00Z"));
        challengeParticipantRepository.saveAllAndFlush(List.of(older, newer));
        entityManager.clear();

        var ordered = challengeParticipantRepository.findByAccount_IdOrderByJoinedAtDesc(creator.getId());

        assertThat(ordered).extracting(ChallengeParticipant::getJoinedAt)
                .containsExactly(OffsetDateTime.parse("2024-02-01T00:00:00Z"), OffsetDateTime.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void countByChallenge_Id_countsParticipants() {
        var creator = persistAccount("count@example.com");
        var participant2 = persistAccount("second@example.com");
        var challenge = persistChallenge(creator);
        challengeParticipantRepository.saveAllAndFlush(List.of(
                new ChallengeParticipant(challenge, creator, OffsetDateTime.parse("2024-01-01T00:00:00Z")),
                new ChallengeParticipant(challenge, participant2, OffsetDateTime.parse("2024-01-02T00:00:00Z"))));

        assertThat(challengeParticipantRepository.countByChallenge_Id(challenge.getId())).isEqualTo(2);
    }

    @Test
    void uniqueParticipantConstraint_isEnforcedAtFlush() {
        var creator = persistAccount("unique@example.com");
        var challenge = persistChallenge(creator);
        challengeParticipantRepository.saveAndFlush(
                new ChallengeParticipant(challenge, creator, OffsetDateTime.parse("2024-01-01T00:00:00Z")));
        var duplicate = new ChallengeParticipant(challenge, creator, OffsetDateTime.parse("2024-01-02T00:00:00Z"));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> challengeParticipantRepository.saveAndFlush(duplicate));
    }
}
