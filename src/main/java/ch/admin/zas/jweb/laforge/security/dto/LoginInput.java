package ch.admin.zas.jweb.laforge.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corps de {@code POST /auth/sessions} (connexion). */
public record LoginInput(
        @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(min = 1, max = 200) String password) {
}
