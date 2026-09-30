package ch.admin.zas.jweb.laforge.collective.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /challenges/join}. */
public record JoinChallengeInput(@NotBlank @Size(min = 32, max = 128) String joinCode) {
}
