package com.example.elibrary.shared.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

/**
 * UUIDs are stored as canonical VARCHAR(36) values, keeping the JPA mapping independent of
 * dialect-specific native UUID handling.
 */
@Converter(autoApply = true)
public class UuidVarcharConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(UUID attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public UUID convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UUID.fromString(dbData);
    }
}
