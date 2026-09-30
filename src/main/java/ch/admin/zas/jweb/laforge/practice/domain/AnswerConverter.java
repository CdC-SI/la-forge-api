package ch.admin.zas.jweb.laforge.practice.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Persiste la réponse polymorphe (voir {@link Answer}) en {@code jsonb}. */
@Converter
@Component
public class AnswerConverter extends JsonAttributeConverter<Answer> {

    public AnswerConverter(ObjectMapper objectMapper) {
        super(objectMapper, Answer.class);
    }
}
