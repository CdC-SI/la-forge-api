package ch.admin.zas.jweb.laforge.security.service;

import java.util.UUID;

/**
 * Publié après le commit de l'émission d'un jeton de vérification (inscription ou renvoi), afin
 * de déclencher l'envoi du courriel hors de la transaction qui a persisté le jeton.
 */
public record VerificationTokenIssuedEvent(UUID accountId, String email, String displayName, String rawToken) {
}
