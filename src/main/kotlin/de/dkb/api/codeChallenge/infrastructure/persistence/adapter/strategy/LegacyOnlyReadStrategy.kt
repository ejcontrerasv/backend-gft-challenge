package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.LegacyTypeParser
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.LegacyUserEntity
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.LegacyUserJpaRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * LEGACY_ONLY read strategy for emergency rollback scenarios.
 *
 * This strategy:
 * - Reads ONLY from the legacy table (users_legacy)
 * - Does NOT perform any migration to the new table
 * - Does NOT write to the new table
 * - Converts legacy types to domain objects in-memory only (no persistence)
 *
 * Use this strategy when the new implementation is failing or corrupted
 * to ensure system stability by isolating from the new table completely.
 */
@Component
class LegacyOnlyReadStrategy(
    private val legacyUserJpaRepository: LegacyUserJpaRepository,
    private val categoryResolutionService: CategoryResolutionService,
    private val clock: Clock,
) : UserReadStrategy {

    override val type: ReadSource = ReadSource.LEGACY_ONLY

    override fun findById(id: UserId): User? {
        logger.debug { "Read strategy: LEGACY_ONLY for user ${id.value} - no migration will be performed" }
        val legacyUser = legacyUserJpaRepository.findById(id.value).orElse(null)
        return legacyUser?.let { convertLegacyToUserWithoutMigration(it) }
    }

    override fun existsById(id: UserId): Boolean = legacyUserJpaRepository.existsById(id.value)

    override fun count(): Long = legacyUserJpaRepository.findAll().size.toLong()

    override fun findAll(): List<User> = legacyUserJpaRepository.findAll()
        .mapNotNull { legacy ->
            try {
                convertLegacyToUserWithoutMigration(legacy)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to convert legacy user ${legacy.id} during findAll" }
                null
            }
        }

    /**
     * Converts a legacy user entity to a domain User object WITHOUT triggering migration.
     * This is a read-only, in-memory conversion that does not persist anything to the new table.
     *
     * Uses CategoryResolutionService directly to resolve categories from legacy types,
     * but does NOT call LegacyDataMigrator (which would log migration messages and could persist data).
     */
    private fun convertLegacyToUserWithoutMigration(legacyUser: LegacyUserEntity): User? {
        val legacyTypesString = legacyUser.getNotificationsAsString()
        logger.debug { "Converting legacy user ${legacyUser.id} with types: '$legacyTypesString' (no migration)" }

        val typeCodes = LegacyTypeParser.parse(legacyTypesString)

        if (typeCodes.isEmpty()) {
            logger.warn { "No valid types found for legacy user ${legacyUser.id}" }
            return null
        }

        // Resolve categories directly without migration (read-only operation)
        val categories = categoryResolutionService.resolveCategoriesFromLegacyTypes(typeCodes)

        if (categories.isEmpty()) {
            logger.warn { "Could not resolve any categories for legacy user ${legacyUser.id} with types: $typeCodes" }
            return null
        }

        // Create subscriptions in-memory only (not persisted)
        val subscriptions = categories.map { category ->
            CategorySubscription(
                category = category,
                subscribedAt = Instant.now(clock),
                active = true,
            )
        }.toSet()

        logger.debug { "Legacy user ${legacyUser.id} converted to ${categories.size} categories (no migration performed)" }

        return User(id = UserId(legacyUser.id), subscriptions = subscriptions)
    }
}
