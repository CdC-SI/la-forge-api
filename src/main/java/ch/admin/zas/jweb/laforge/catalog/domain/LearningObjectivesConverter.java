package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Persiste {@code learningObjectives} en {@code jsonb} (liste ordonnée de chaînes). */
@Converter
@Component
public class LearningObjectivesConverter extends JsonAttributeConverter<List<String>> {

    public LearningObjectivesConverter(ObjectMapper objectMapper) {
        super(objectMapper, new TypeReference<List<String>>() {
        });
    }
}
