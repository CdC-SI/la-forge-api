package ch.admin.zas.jweb.laforge.authoring.repository;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Accès aux brouillons d'exercices. */
public interface DraftRepository extends JpaRepository<Draft, UUID>, JpaSpecificationExecutor<Draft> {

    /** Brouillon non publié pour un exercice donné (au plus un à la fois, imposé par le contrat). */
    Optional<Draft> findByExerciseIdAndStateNot(UUID exerciseId, DraftState excludedState);

    List<Draft> findByAuthorId(UUID authorId);

    List<Draft> findByState(DraftState state);

    boolean existsByExercise_IdAndAuthor_Id(UUID exerciseId, UUID authorId);
}
