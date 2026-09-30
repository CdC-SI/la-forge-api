package ch.admin.zas.jweb.laforge.collective.dto;

import ch.admin.zas.jweb.laforge.practice.domain.Answer;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Réponse partagée entre participants ayant soumis dans un défi (schéma {@code SharedResponse}).
 * {@code id} est l'identifiant de la réponse partagée (celui de l'inscription au défi), jamais
 * celui de la tentative privée sous-jacente.
 */
public record SharedResponseDto(UUID id, String displayName, Answer answer, OffsetDateTime submittedAt) {
}
