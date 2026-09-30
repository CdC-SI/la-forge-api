package ch.admin.zas.jweb.laforge.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /auth/verifications/resend}. */
public record ResendVerificationInput(@NotBlank @Email @Size(max = 320) String email) {
}
