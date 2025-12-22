package de.dkb.api.codeChallenge.application.service

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
import de.dkb.api.codeChallenge.domain.service.LegacyTypeParser
import mu.KotlinLogging
import java.time.Clock
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Default implementation of LegacyDataMigrator.
 * Converts legacy notification types to category-based subscriptions.
 */
class DefaultLegacyDataMigrator(private val categoryResolutionService: CategoryResolutionService, private val clock: Clock) :
    LegacyDataMigrator {

    override fun migrateUserTypes(userId: UserId, legacyTypes: String): Set<CategorySubscription> {
        logger.info { "Migrating user ${userId.value}: '$legacyTypes'" }

        val typeCodes = LegacyTypeParser.parse(legacyTypes)

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
                subscribedAt = Instant.now(clock),
                active = true,
            )
        }.toSet()

        logger.info { "Successfully migrated user ${userId.value}: ${typeCodes.size} types -> ${categories.size} categories" }

        return subscriptions
    }
}
