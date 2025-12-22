package de.dkb.api.codeChallenge.domain.service

/**
 * Parser object for parsing legacy notification type strings.
 * Centralizes the parsing logic to avoid code duplication.
 */
object LegacyTypeParser {

    private const val TYPE_SEPARATOR = ";"

    /**
     * Parse a semicolon-separated string of legacy notification types.
     *
     * Examples:
     * - "type1" -> setOf("type1")
     * - "type1;type2;type3" -> setOf("type1", "type2", "type3")
     * - "TYPE1;Type2" -> setOf("type1", "type2") (normalized to lowercase)
     * - "" -> emptySet()
     * - "  type1 ; type2  " -> setOf("type1", "type2") (trimmed)
     *
     * @param legacyTypes semicolon-separated string of notification types
     * @return set of normalized (lowercase, trimmed) type codes
     */
    fun parse(legacyTypes: String): Set<String> {
        if (legacyTypes.isBlank()) {
            return emptySet()
        }

        return legacyTypes
            .split(TYPE_SEPARATOR)
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
