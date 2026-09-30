package ch.admin.zas.jweb.laforge.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Base réutilisable pour convertir un sous-document immuable (fichiers, indices, corrigé,
 * réponse, etc.) en colonne {@code jsonb}. Les sous-classes sont annotées {@code @Converter} et
 * injectées par Spring (le conteneur de beans Hibernate est activé par défaut par Spring Boot),
 * ce qui permet de réutiliser l'{@link ObjectMapper} configuré de l'application plutôt que d'en
 * créer un nouveau par entité. Accepte aussi bien un type simple (record) qu'un type générique
 * (ex. {@code List<CodeFile>}) via {@link TypeReference}.
 *
 * @param <T> type Java du sous-document (record, liste de records ou hiérarchie scellée)
 */
@Converter
public abstract class JsonAttributeConverter<T> implements AttributeConverter<T, String> {

    private final ObjectMapper objectMapper;
    private final JavaType javaType;

    protected JsonAttributeConverter(ObjectMapper objectMapper, Class<T> type) {
        this.objectMapper = objectMapper;
        this.javaType = objectMapper.constructType(type);
    }

    protected JsonAttributeConverter(ObjectMapper objectMapper, TypeReference<T> typeReference) {
        this.objectMapper = objectMapper;
        this.javaType = objectMapper.constructType(typeReference.getType());
    }

    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JacksonException e) {
            throw new IllegalStateException("Échec de sérialisation JSON pour " + javaType, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return (T) objectMapper.readValue(dbData, javaType);
        } catch (JacksonException e) {
            throw new IllegalStateException("Échec de désérialisation JSON pour " + javaType, e);
        }
    }
}
