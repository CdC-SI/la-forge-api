package ch.admin.zas.jweb.laforge.practice.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Projection agrégée par thème pour {@code GET /me/progress}, calculée en base. */
public interface TopicProgressProjection {

    UUID getTopicId();

    long getSubmittedAttempts();

    long getCorrectChoiceAttempts();

    long getGradedChoiceAttempts();

    OffsetDateTime getLastPracticedAt();
}
