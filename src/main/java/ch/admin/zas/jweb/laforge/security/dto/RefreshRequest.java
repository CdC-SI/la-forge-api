package ch.admin.zas.jweb.laforge.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /auth/sessions/refresh} et {@code DELETE /auth/sessions}. */
public record RefreshRequest(@NotBlank @Size(min = 1, max = 500) String refreshToken) {
}
