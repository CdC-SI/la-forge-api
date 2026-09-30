package ch.admin.zas.jweb.laforge.profile.domain;

import ch.admin.zas.jweb.laforge.common.persistence.JsonAttributeConverter;
import ch.admin.zas.jweb.laforge.profile.dto.StackEntryDto;
import jakarta.persistence.Converter;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Persiste la pile technologique suivie par l'apprenant en {@code jsonb}. */
@Converter
@Component
public class StackEntriesConverter extends JsonAttributeConverter<List<StackEntryDto>> {

    public StackEntriesConverter(ObjectMapper objectMapper) {
        super(objectMapper, new TypeReference<List<StackEntryDto>>() {
        });
    }
}
