package de.dkb.api.codeChallenge.application.service

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

class DefaultLegacyDataMigrator(
    private val categoryResolutionService: CategoryResolutionService
) : LegacyDataMigrator {

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

