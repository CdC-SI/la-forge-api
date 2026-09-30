package ch.admin.zas.jweb.laforge.discovery.dto;

import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Contenu rédigé d'une fiche de veille, soumis par un {@code REVIEWER}/{@code ADMIN}. */
public record ArticleInput(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String summary,
        @NotNull @Size(max = 20) List<UUID> topicIds,
        @Valid @Size(max = 10) List<TechnologyRequirement> technologies,
        @NotBlank @Size(max = 20000) String bodyMarkdown,
        @Valid @NotEmpty @Size(max = 20) List<Source> sources,
        @Valid @NotNull @Size(max = 10) List<ExerciseReferenceDto> relatedExercises) {
}
