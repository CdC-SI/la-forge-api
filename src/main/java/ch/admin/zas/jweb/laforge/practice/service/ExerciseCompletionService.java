package ch.admin.zas.jweb.laforge.practice.service;

import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture groupée des exercices déjà soumis par l'apprenant, toutes versions confondues. */
@Service
@Transactional(readOnly = true)
public class ExerciseCompletionService {

    private final AttemptRepository attemptRepository;

    public ExerciseCompletionService(AttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public Set<UUID> completedExerciseIds(UUID learnerId, Collection<UUID> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(attemptRepository.findCompletedExerciseIds(learnerId, Set.copyOf(exerciseIds)));
    }
}
