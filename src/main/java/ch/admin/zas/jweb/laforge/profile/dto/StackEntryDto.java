package ch.admin.zas.jweb.laforge.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Technologie suivie par l'apprenant, avec version courante et version visée. */
public record StackEntryDto(
        @NotBlank @Size(max = 60) String technology,
        @NotBlank @Size(max = 40) String currentVersion,
        @Size(max = 40) String targetVersion) {
}
