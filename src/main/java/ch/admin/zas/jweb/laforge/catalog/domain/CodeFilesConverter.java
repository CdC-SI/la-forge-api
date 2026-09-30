package ch.admin.zas.jweb.laforge.catalog.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Persiste {@code files} en {@code jsonb}. */
@Converter
@Component
public class CodeFilesConverter extends JsonAttributeConverter<List<CodeFile>> {

    public CodeFilesConverter(ObjectMapper objectMapper) {
        super(objectMapper, new TypeReference<List<CodeFile>>() {
        });
    }
}
