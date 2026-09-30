package ch.admin.zas.jweb.laforge.practice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseCompletionServiceTest {

    @Mock
    private AttemptRepository attemptRepository;

    @Test
    void completedExerciseIds_pageVide_neConsultePasLeDepot() {
        assertThat(new ExerciseCompletionService(attemptRepository)
                .completedExerciseIds(UUID.randomUUID(), List.of())).isEmpty();
        verifyNoInteractions(attemptRepository);
    }

    @Test
    void completedExerciseIds_dedupliqueEtTransmetLeCompteEnUnSeulLot() {
        var learnerId = UUID.randomUUID();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        when(attemptRepository.findCompletedExerciseIds(learnerId, Set.of(first, second))).thenReturn(Set.of(first));

        var result = new ExerciseCompletionService(attemptRepository)
                .completedExerciseIds(learnerId, List.of(first, second, first));

        assertThat(result).containsExactly(first);
        verify(attemptRepository).findCompletedExerciseIds(learnerId, Set.of(first, second));
        verifyNoMoreInteractions(attemptRepository);
    }
}
