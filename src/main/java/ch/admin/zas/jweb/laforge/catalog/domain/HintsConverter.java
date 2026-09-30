package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Persiste {@code hints} en {@code jsonb}. */
@Converter
@Component
public class HintsConverter extends JsonAttributeConverter<List<Hint>> {

    public HintsConverter(ObjectMapper objectMapper) {
        super(objectMapper, new TypeReference<List<Hint>>() {
        });
    }
}
