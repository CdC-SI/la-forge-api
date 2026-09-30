package ch.admin.zas.jweb.laforge.profile.dto;

import java.util.List;

/**
 * Progression personnelle (schéma {@code Progress}). Compteurs de réussite limités aux réponses
 * à choix corrigées automatiquement ; aucune note générée par IA ni classement entre personnes.
 */
public record ProgressDto(
        int submittedAttempts, int activeDaysLast30Days, int dueReviewCount, List<TopicProgressDto> topics) {
}
