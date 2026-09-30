package ch.admin.zas.jweb.laforge.catalog.repository;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exercise e where e.id = :id")
    Optional<Exercise> findByIdForUpdate(@Param("id") UUID id);
}
