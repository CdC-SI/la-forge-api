package ch.admin.zas.jweb.laforge.profile.dto;

import ch.admin.zas.jweb.laforge.practice.repository.TopicProgressProjection;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Compteurs de réussite personnels pour un thème (schéma {@code TopicProgress}). */
public record TopicProgressDto(
        UUID topicId, int submittedAttempts, int correctChoiceAttempts, int gradedChoiceAttempts,
        OffsetDateTime lastPracticedAt) {

    public static TopicProgressDto from(TopicProgressProjection projection) {
        return new TopicProgressDto(
                projection.getTopicId(),
                (int) projection.getSubmittedAttempts(),
                (int) projection.getCorrectChoiceAttempts(),
                (int) projection.getGradedChoiceAttempts(),
                projection.getLastPracticedAt());
    }
}
