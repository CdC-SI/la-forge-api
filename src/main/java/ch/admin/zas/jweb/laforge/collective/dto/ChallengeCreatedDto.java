package ch.admin.zas.jweb.laforge.collective.dto;

/**
 * Réponse de {@code POST /challenges} : défi créé et code d'invitation en clair, rendu une seule
 * fois. Ne jamais journaliser {@code joinCode}.
 */
public record ChallengeCreatedDto(ChallengeDto challenge, String joinCode) {
}
