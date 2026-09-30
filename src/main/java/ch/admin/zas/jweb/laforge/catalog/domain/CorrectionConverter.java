package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Persiste {@code correction} en {@code jsonb}. */
@Converter
@Component
public class CorrectionConverter extends JsonAttributeConverter<Correction> {

    public CorrectionConverter(ObjectMapper objectMapper) {
        super(objectMapper, Correction.class);
    }
}
