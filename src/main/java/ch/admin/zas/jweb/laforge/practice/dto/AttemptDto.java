package ch.admin.zas.jweb.laforge.practice.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.practice.domain.Answer;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Projection privée d'une tentative, jamais visible d'un autre compte que son propriétaire. */
public record AttemptDto(
        UUID id,
        UUID exerciseId,
        int exerciseVersion,
        UUID challengeId,
        UUID reviewItemId,
        AttemptStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime submittedAt,
        Answer answer,
        List<Hint> revealedHints,
        boolean debriefAvailable) {

    public static AttemptDto from(Attempt attempt) {
        var version = attempt.getExerciseVersion();
        var allHints = version.getHints();
        var revealed = attempt.getRevealedHintLevels().stream().map(level -> allHints.get(level - 1)).toList();
        return new AttemptDto(
                attempt.getId(),
                version.getExercise().getId(),
                version.getVersionNumber(),
                attempt.getChallengeId(),
                attempt.getReviewItemId(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getAnswer(),
                revealed,
                attempt.isDebriefAvailable());
    }
}
