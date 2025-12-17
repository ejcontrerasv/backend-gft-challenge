package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Domain service for migrating legacy type-based data to category-based model.
 */
interface LegacyDataMigrator {
    /**
     * Migrate legacy notification types string to category subscriptions
     *
     * Examples:
     * - "type1" -> [CategoryA]
     * - "type1;type2;type3" -> [CategoryA]
     * - "type1;type5" -> [CategoryA, CategoryB]
     * - "type1;type2;type4;type5" -> [CategoryA, CategoryB]
     */
    fun migrateUserTypes(userId: UserId, legacyTypes: String): Set<CategorySubscription>
}

class DefaultLegacyDataMigrator(private val categoryResolutionService: CategoryResolutionService) : LegacyDataMigrator {

    companion object {
        private const val TYPE_SEPARATOR = ";"
    }

    override fun migrateUserTypes(userId: UserId, legacyTypes: String): Set<CategorySubscription> {
        logger.info { "Migrating user ${userId.value}: '$legacyTypes'" }

        val typeCodes = parseLegacyTypes(legacyTypes)

        if (typeCodes.isEmpty()) {
            logger.warn { "No valid types found for user ${userId.value}" }
            return emptySet()
        }

        val categories = categoryResolutionService.resolveCategoriesFromLegacyTypes(typeCodes)

        if (categories.isEmpty()) {
            logger.error { "Could not resolve any categories for user ${userId.value} with types: $typeCodes" }
            return emptySet()
        }

        val subscriptions = categories.map { category ->
            CategorySubscription(
                category = category,
                subscribedAt = Instant.now(),
                active = true,
            )
        }.toSet()

        logger.info { "Successfully migrated user ${userId.value}: ${typeCodes.size} types -> ${categories.size} categories" }

        return subscriptions
    }

    /**
     * Parse legacy types string handling various edge cases:
     * - Empty/null strings
     * - Trailing/leading semicolons: ";type1;"
     * - Extra whitespace: " type1 ; type2 "
     * - Duplicates: "type1;type1"
     * - Case variations: "TYPE1" vs "type1"
     */
    private fun parseLegacyTypes(legacyTypes: String): Set<String> {
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

/**
 * Exception thrown when migration fails
 */
class MigrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
