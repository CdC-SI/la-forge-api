package ch.admin.zas.jweb.laforge.collective.dto;

import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.domain.ChallengeState;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projection de lecture d'un défi collectif (schéma {@code Challenge}). {@code participantCount}
 * et {@code responsesUnlocked} ne sont pas des colonnes de l'entité : ils sont calculés par le
 * service à la lecture (comptage des inscriptions, tentative soumise de l'appelant).
 */
public record ChallengeDto(
        UUID id,
        String title,
        ExerciseSummaryDto exercise,
        UUID creatorId,
        OffsetDateTime closesAt,
        ChallengeState state,
        int participantCount,
        boolean responsesUnlocked) {

    public static ChallengeDto from(
            Challenge challenge, int participantCount, boolean responsesUnlocked, boolean completed) {
        return new ChallengeDto(
                challenge.getId(),
                challenge.getTitle(),
                ExerciseSummaryDto.from(challenge.getExerciseVersion(), completed),
                challenge.getCreator().getId(),
                challenge.getClosesAt(),
                challenge.getState(),
                participantCount,
                responsesUnlocked);
    }
}
