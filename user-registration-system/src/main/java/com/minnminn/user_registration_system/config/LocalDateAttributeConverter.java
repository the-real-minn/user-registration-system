package com.minnminn.user_registration_system.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * SQLite-safe LocalDate storage as ISO text, with flexible read for legacy Excel values.
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, String> {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter[] EXCEL_FORMATS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("M/d/yyyy")
    };

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : ISO.format(attribute);
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String value = dbData.trim();
        try {
            return LocalDate.parse(value, ISO);
        } catch (DateTimeParseException ignored) {
            // continue
        }
        // Epoch millis accidentally stored as number/text
        if (value.matches("\\d{11,13}")) {
            long ms = Long.parseLong(value);
            return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        for (DateTimeFormatter fmt : EXCEL_FORMATS) {
            try {
                return LocalDate.parse(value, fmt.withLocale(Locale.ENGLISH));
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }
}
