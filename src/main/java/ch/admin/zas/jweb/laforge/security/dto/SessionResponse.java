package ch.admin.zas.jweb.laforge.security.dto;

import ch.admin.zas.jweb.laforge.profile.dto.UserDto;

/** Corps de réponse {@code Session} : jeton d'accès, jeton de renouvellement et profil courant. */
public record SessionResponse(
        String accessToken, String refreshToken, String tokenType, long expiresInSeconds, UserDto user) {

    public static SessionResponse bearer(String accessToken, String refreshToken, long expiresInSeconds, UserDto user) {
        return new SessionResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
