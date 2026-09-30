package ch.admin.zas.jweb.laforge.catalog.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Projection résumée d'une version publiée, utilisée dans les listes et le tableau de bord. */
public record ExerciseSummaryDto(
        UUID id,
        int version,
        String title,
        ExerciseType type,
        Difficulty difficulty,
        List<UUID> topicIds,
        int estimatedMinutes,
        List<TechnologyRequirement> technologies,
        OffsetDateTime publishedAt,
        boolean completed) {

    public static ExerciseSummaryDto from(ExerciseVersion version, boolean completed) {
        var topicIds = version.getTopics().stream().map(topic -> topic.getId()).collect(Collectors.toList());
        return new ExerciseSummaryDto(
                version.getExercise().getId(),
                version.getVersionNumber(),
                version.getTitle(),
                version.getType(),
                version.getDifficulty(),
                topicIds,
                version.getEstimatedMinutes(),
                version.getTechnologies(),
                version.getPublishedAt(),
                completed);
    }
}
