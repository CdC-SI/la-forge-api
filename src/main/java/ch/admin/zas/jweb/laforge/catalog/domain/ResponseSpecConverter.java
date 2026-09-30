package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Persiste {@code responseSpec} en {@code jsonb}. */
@Converter
@Component
public class ResponseSpecConverter extends JsonAttributeConverter<ResponseSpec> {

    public ResponseSpecConverter(ObjectMapper objectMapper) {
        super(objectMapper, ResponseSpec.class);
    }
}
