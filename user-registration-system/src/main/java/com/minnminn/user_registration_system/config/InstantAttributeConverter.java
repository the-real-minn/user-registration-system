package com.minnminn.user_registration_system.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Store Instant as ISO-8601 text in SQLite (avoids epoch parse issues).
 */
@Converter(autoApply = true)
public class InstantAttributeConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String value = dbData.trim();
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        if (value.matches("\\d{11,13}")) {
            return Instant.ofEpochMilli(Long.parseLong(value));
        }
        return null;
    }
}
