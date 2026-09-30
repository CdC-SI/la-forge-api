package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Persiste {@code technologies} en {@code jsonb}. */
@Converter
@Component
public class TechnologyRequirementsConverter extends JsonAttributeConverter<List<TechnologyRequirement>> {

    public TechnologyRequirementsConverter(ObjectMapper objectMapper) {
        super(objectMapper, new TypeReference<List<TechnologyRequirement>>() {
        });
    }
}
