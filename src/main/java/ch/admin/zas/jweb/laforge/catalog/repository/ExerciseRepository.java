package ch.admin.zas.jweb.laforge.catalog.repository;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
}
