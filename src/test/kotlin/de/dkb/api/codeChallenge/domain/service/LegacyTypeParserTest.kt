package de.dkb.api.codeChallenge.domain.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LegacyTypeParser")
class LegacyTypeParserTest {

    @Nested
    @DisplayName("parse")
    inner class Parse {

        @Test
        @DisplayName("should parse single type")
        fun shouldParseSingleType() {
            val result = LegacyTypeParser.parse("type1")

            assertEquals(setOf("type1"), result)
        }

        @Test
        @DisplayName("should parse multiple types separated by semicolon")
        fun shouldParseMultipleTypes() {
            val result = LegacyTypeParser.parse("type1;type2;type3")

            assertEquals(setOf("type1", "type2", "type3"), result)
        }

        @Test
        @DisplayName("should normalize to lowercase")
        fun shouldNormalizeToLowercase() {
            val result = LegacyTypeParser.parse("TYPE1;Type2;tYpE3")

            assertEquals(setOf("type1", "type2", "type3"), result)
        }

        @Test
        @DisplayName("should trim whitespace from each type")
        fun shouldTrimWhitespace() {
            val result = LegacyTypeParser.parse("  type1 ; type2  ;  type3  ")

            assertEquals(setOf("type1", "type2", "type3"), result)
        }

        @Test
        @DisplayName("should return empty set for blank string")
        fun shouldReturnEmptyForBlankString() {
            assertEquals(emptySet<String>(), LegacyTypeParser.parse(""))
            assertEquals(emptySet<String>(), LegacyTypeParser.parse("   "))
        }

        @Test
        @DisplayName("should filter out empty segments")
        fun shouldFilterEmptySegments() {
            val result = LegacyTypeParser.parse("type1;;type2;;;type3")

            assertEquals(setOf("type1", "type2", "type3"), result)
        }

        @Test
        @DisplayName("should handle single type with semicolon")
        fun shouldHandleSingleTypeWithSemicolon() {
            val result = LegacyTypeParser.parse("type1;")

            assertEquals(setOf("type1"), result)
        }

        @Test
        @DisplayName("should deduplicate repeated types")
        fun shouldDeduplicateRepeatedTypes() {
            val result = LegacyTypeParser.parse("type1;type1;type2;type2")

            assertEquals(setOf("type1", "type2"), result)
        }

        @Test
        @DisplayName("should handle mixed case duplicates")
        fun shouldHandleMixedCaseDuplicates() {
            val result = LegacyTypeParser.parse("TYPE1;type1;Type1")

            assertEquals(setOf("type1"), result)
        }
    }
}
