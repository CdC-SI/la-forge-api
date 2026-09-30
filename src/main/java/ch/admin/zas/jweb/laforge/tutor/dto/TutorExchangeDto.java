package ch.admin.zas.jweb.laforge.tutor.dto;

import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.tutor.domain.TutorExchange;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Projection de lecture d'un échange avec le tuteur (schéma {@code TutorExchange}). */
public record TutorExchangeDto(
        UUID id, String question, String answerMarkdown, List<Source> sources, OffsetDateTime createdAt, boolean generatedByAi) {

    public static TutorExchangeDto from(TutorExchange exchange) {
        return new TutorExchangeDto(
                exchange.getId(),
                exchange.getQuestion(),
                exchange.getAnswerMarkdown(),
                exchange.getSources(),
                exchange.getCreatedAt(),
                exchange.isGeneratedByAi());
    }
}
