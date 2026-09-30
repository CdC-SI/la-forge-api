package ch.admin.zas.jweb.laforge.collective.repository;

import ch.admin.zas.jweb.laforge.collective.domain.ChallengeParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, UUID> {

    Optional<ChallengeParticipant> findByChallenge_IdAndAccount_Id(UUID challengeId, UUID accountId);

    List<ChallengeParticipant> findByAccount_IdOrderByJoinedAtDesc(UUID accountId);

    long countByChallenge_Id(UUID challengeId);
}
