package ch.admin.zas.jweb.laforge.catalog.repository;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Les requêtes combinées de {@code GET /exercises} (type, difficulté, thème, technologie, texte
 * libre) sont construites via {@link JpaSpecificationExecutor} par le service du domaine
 * {@code catalog-feature} ; ce dépôt ne porte que les accès directs par identifiant/version.
 */
public interface ExerciseVersionRepository
        extends JpaRepository<ExerciseVersion, UUID>, JpaSpecificationExecutor<ExerciseVersion> {

    Optional<ExerciseVersion> findByExercise_IdAndVersionNumber(UUID exerciseId, int versionNumber);

    Optional<ExerciseVersion> findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(UUID exerciseId);
}
