package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Legacy notification type enum.
 * Used only for reading from users_legacy table during migration.
 * Will be removed once migration is complete.
 */
@Suppress("EnumEntryName")
enum class LegacyNotificationType {
    type1,
    type2,
    type3,
    type4,
    type5,
}

/**
 * JPA Converter for legacy notification types stored as semicolon-separated string.
 * Used only for users_legacy table during dual-write migration.
 */
@Converter
class LegacyNotificationTypeSetConverter : AttributeConverter<MutableSet<LegacyNotificationType>, String> {

    override fun convertToDatabaseColumn(valueSet: MutableSet<LegacyNotificationType>?): String =
        valueSet.orEmpty()
            .joinToString(separator = ";") { it.name }

    override fun convertToEntityAttribute(databaseString: String?): MutableSet<LegacyNotificationType> {
        if (databaseString.isNullOrBlank()) {
            return mutableSetOf()
        }
        return databaseString
            .split(";")
            .filter { it.isNotBlank() }
            .mapNotNull {
                try {
                    LegacyNotificationType.valueOf(it.trim())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .toMutableSet()
    }
}
