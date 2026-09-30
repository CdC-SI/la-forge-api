package ch.admin.zas.jweb.laforge.catalog.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Technologie requise ; version, statut et notes sont facultatifs. */
public record TechnologyRequirement(
        @NotBlank @Size(max = 200) String technology,
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 40) String minimumVersion,
        FeatureStatus featureStatus,
        @Size(max = 4000) String notes) {
}
