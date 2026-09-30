package ch.admin.zas.jweb.laforge.tutor.repository;

import ch.admin.zas.jweb.laforge.tutor.domain.TutorExchange;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès aux échanges avec le tuteur IA, ordonnés par date de création croissante puis id. */
public interface TutorExchangeRepository extends JpaRepository<TutorExchange, UUID> {

    List<TutorExchange> findByAttemptIdOrderByCreatedAtAscIdAsc(UUID attemptId);
}
