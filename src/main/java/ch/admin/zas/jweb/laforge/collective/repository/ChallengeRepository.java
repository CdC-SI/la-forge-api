package ch.admin.zas.jweb.laforge.collective.repository;

import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID>, JpaSpecificationExecutor<Challenge> {

    Optional<Challenge> findByJoinCodeHash(String joinCodeHash);

    /** Défis ouverts créés ou déjà rejoints par le compte, pour le tableau de bord. */
    @Query("""
            SELECT DISTINCT c FROM Challenge c LEFT JOIN ChallengeParticipant p ON p.challenge = c
            WHERE (c.creator.id = :accountId OR p.account.id = :accountId)
              AND c.state = ch.admin.zas.jweb.laforge.collective.domain.ChallengeState.OPEN
            ORDER BY c.closesAt ASC
            """)
    List<Challenge> findOpenChallengesForAccount(@Param("accountId") UUID accountId);
}
