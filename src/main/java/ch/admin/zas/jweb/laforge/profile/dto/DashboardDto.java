package ch.admin.zas.jweb.laforge.profile.dto;

import ch.admin.zas.jweb.laforge.catalog.dto.ExerciseSummaryDto;
import ch.admin.zas.jweb.laforge.collective.dto.ChallengeDto;
import ch.admin.zas.jweb.laforge.discovery.dto.ArticleSummaryDto;
import java.util.List;

/**
 * Suggestions personnelles (schéma {@code Dashboard}). Les tableaux peuvent être vides ; les défis
 * retournés sont ceux créés ou déjà rejoints par l'utilisateur.
 */
public record DashboardDto(
        List<ExerciseSummaryDto> recommendedExercises,
        List<ArticleSummaryDto> discoveries,
        int dueReviewCount,
        List<ChallengeDto> openChallenges,
        boolean aiAvailable) {
}
