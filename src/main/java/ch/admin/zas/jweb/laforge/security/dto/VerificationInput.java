package ch.admin.zas.jweb.laforge.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /auth/verifications}. */
public record VerificationInput(@NotBlank @Size(min = 1, max = 200) String token) {
}
