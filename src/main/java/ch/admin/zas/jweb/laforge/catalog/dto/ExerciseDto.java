package ch.admin.zas.jweb.laforge.catalog.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.CodeFile;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Projection apprenant d'une version publiée : ni corrigé, ni bonne réponse, ni contenu des
 * indices non débloqués (seul leur nombre est exposé).
 */
public record ExerciseDto(
        UUID id,
        int version,
        String title,
        ExerciseType type,
        Difficulty difficulty,
        List<UUID> topicIds,
        int estimatedMinutes,
        List<TechnologyRequirement> technologies,
        OffsetDateTime publishedAt,
        String promptMarkdown,
        List<String> learningObjectives,
        List<CodeFile> files,
        ResponseSpec responseSpec,
        int hintCount,
        boolean completed) {

    public static ExerciseDto from(ExerciseVersion version, boolean completed) {
        var topicIds = version.getTopics().stream().map(topic -> topic.getId()).collect(Collectors.toList());
        return new ExerciseDto(
                version.getExercise().getId(),
                version.getVersionNumber(),
                version.getTitle(),
                version.getType(),
                version.getDifficulty(),
                topicIds,
                version.getEstimatedMinutes(),
                version.getTechnologies(),
                version.getPublishedAt(),
                version.getPromptMarkdown(),
                version.getLearningObjectives(),
                version.getFiles(),
                version.getResponseSpec(),
                version.getHints().size(),
                completed);
    }
}
