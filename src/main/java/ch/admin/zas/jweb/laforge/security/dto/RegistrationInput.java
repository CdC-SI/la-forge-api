package ch.admin.zas.jweb.laforge.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /auth/registrations}. */
public record RegistrationInput(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 200) String password,
        @NotBlank @Size(min = 1, max = 200) String displayName) {
}
