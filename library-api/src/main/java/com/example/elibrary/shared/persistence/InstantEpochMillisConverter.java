package com.example.elibrary.shared.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;

/**
 * Instants are persisted as epoch milliseconds. SQLite has no native timestamp type and stores
 * timestamps as text/numeric depending on the driver; a BIGINT representation is unambiguous,
 * sortable and identical on PostgreSQL. Domain code truncates to milliseconds so a DB round trip
 * does not change values.
 */
@Converter(autoApply = true)
public class InstantEpochMillisConverter implements AttributeConverter<Instant, Long> {

    @Override
    public Long convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : attribute.toEpochMilli();
    }

    @Override
    public Instant convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : Instant.ofEpochMilli(dbData);
    }
}
