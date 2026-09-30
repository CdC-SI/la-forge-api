package ch.admin.zas.jweb.laforge.catalog.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Technologie et version minimale requises pour aborder un exercice. */
public record TechnologyRequirement(
        @NotBlank @Size(max = 200) String technology,
        @NotBlank @Size(max = 40) String minimumVersion,
        @NotNull FeatureStatus featureStatus,
        @Size(max = 4000) String notes) {
}
