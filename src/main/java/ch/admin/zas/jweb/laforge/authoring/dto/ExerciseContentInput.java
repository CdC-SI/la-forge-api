package ch.admin.zas.jweb.laforge.authoring.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.CodeFile;
import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.Hint;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Contenu progressif d'un brouillon. Seul le titre est requis à la saisie ;
 * la complétude et la cohérence sont vérifiées à la publication.
 */
public record ExerciseContentInput(
        @NotBlank @Size(max = 200) String title,
        ExerciseType type,
        Difficulty difficulty,
        @Size(max = 20) List<@NotNull UUID> topicIds,
        @Min(1) @Max(60) Integer estimatedMinutes,
        @Size(max = 10) List<@NotNull @Valid TechnologyRequirement> technologies,
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 20000) String promptMarkdown,
        @Size(max = 10) List<@NotBlank @Size(max = 200) String> learningObjectives,
        @Size(max = 10) List<@NotNull @Valid CodeFile> files,
        @Valid ResponseSpec responseSpec,
        @Size(max = 5) List<@NotNull @Valid Hint> hints,
        @Valid Correction correction) {

    public ExerciseContentInput {
        topicIds = topicIds == null ? List.of() : List.copyOf(topicIds);
        technologies = technologies == null ? List.of() : List.copyOf(technologies);
        learningObjectives = learningObjectives == null ? List.of() : List.copyOf(learningObjectives);
        files = files == null ? List.of() : List.copyOf(files);
        hints = hints == null ? List.of() : List.copyOf(hints);
    }
}
