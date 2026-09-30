package ch.admin.zas.jweb.laforge.discovery.domain;

import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Persiste les sources citées par une fiche de veille en {@code jsonb}. */
@Converter
@Component
public class ArticleSourcesConverter extends JsonAttributeConverter<List<Source>> {

    public ArticleSourcesConverter(ObjectMapper objectMapper) {
        super(objectMapper, new TypeReference<List<Source>>() {
        });
    }
}
