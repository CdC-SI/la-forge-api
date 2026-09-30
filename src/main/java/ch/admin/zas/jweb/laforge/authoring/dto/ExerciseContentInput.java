package ch.admin.zas.jweb.laforge.authoring.dto;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.catalog.domain.CodeFile;
import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Contenu éditorial complet d'un brouillon (schéma {@code ExerciseContent}), commun à la création,
 * au remplacement et à la projection de lecture. Indices contigus à partir de 1 (vérifié à la
 * soumission en relecture, pas à la simple saisie).
 */
public record ExerciseContentInput(
        @NotBlank @Size(max = 200) String title,
        @NotNull ExerciseType type,
        @NotNull Difficulty difficulty,
        @NotNull @Size(max = 20) List<UUID> topicIds,
        @Min(1) @Max(60) int estimatedMinutes,
        @NotNull @Size(max = 10) List<@Valid TechnologyRequirement> technologies,
        @NotBlank @Size(max = 20000) String promptMarkdown,
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 200) String> learningObjectives,
        @NotNull @Size(max = 10) List<@Valid CodeFile> files,
        @NotNull @Valid ResponseSpec responseSpec,
        @NotNull @Size(max = 5) List<@Valid Hint> hints,
        @NotNull @Valid Correction correction) {

    public static ExerciseContentInput from(Draft draft) {
        return new ExerciseContentInput(
                draft.getTitle(),
                draft.getType(),
                draft.getDifficulty(),
                draft.getTopics().stream().map(BaseEntity::getId).toList(),
                draft.getEstimatedMinutes(),
                draft.getTechnologies(),
                draft.getPromptMarkdown(),
                draft.getLearningObjectives(),
                draft.getFiles(),
                draft.getResponseSpec(),
                draft.getHints(),
                draft.getCorrection());
    }
}
