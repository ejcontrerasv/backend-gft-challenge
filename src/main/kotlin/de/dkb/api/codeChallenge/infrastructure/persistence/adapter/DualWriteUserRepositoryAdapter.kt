package de.dkb.api.codeChallenge.infrastructure.persistence.adapter

import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.infrastructure.config.MigrationProperties
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
import de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy.UserReadStrategy
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.LegacyNotificationType
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.LegacyUserEntity
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.LegacyUserJpaRepository
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Dual-Write implementation of UserRepository for safe migration.
 */
@Component
@Primary
@ConditionalOnProperty(
    name = ["migration.dual-write.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class DualWriteUserRepositoryAdapter(
    private val newUserRepository: UserRepositoryAdapter,
    private val legacyUserJpaRepository: LegacyUserJpaRepository,
    private val migrationProperties: MigrationProperties,
    private val readStrategies: Map<ReadSource, UserReadStrategy>,
) : UserRepository {

    private fun strategy(): UserReadStrategy =
        readStrategies[migrationProperties.dualWrite.readSource]
            ?: readStrategies[ReadSource.NEW_WITH_FALLBACK]
            ?: error("No UserReadStrategy registered")

    /**
     * Read: Strategy based on read-source configuration
     * - NEW_WITH_FALLBACK: Try a new table first, fallback to legacy with on-the-fly migration (default)
     * - NEW_ONLY: Read-only from a new table (after migration complete)
     * - LEGACY_ONLY: Read-only from the legacy table (rollback scenario)
     */
    override fun findById(id: UserId): User? = strategy().findById(id)

    @Transactional
    override fun save(user: User): User {
        /**
         * Write: Dual-write to both new and legacy tables
         */
        logger.debug { "Dual-write save for user ${user.id.value}" }

        try {
            newUserRepository.save(user)
            saveLegacyFormat(user)
            logger.info { "Successfully dual-wrote user ${user.id.value}" }
            return user
        } catch (e: Exception) {
            logger.error(e) { "Dual-write failed for user ${user.id.value}" }
            throw e
        }
    }

    override fun findByCategory(categoryId: CategoryId): List<User> = newUserRepository.findByCategory(categoryId)

    override fun existsById(id: UserId): Boolean = strategy().existsById(id)

    override fun count(): Long = strategy().count()

    override fun findAll(): List<User> = strategy().findAll()

    private fun saveLegacyFormat(user: User) {
        val typeCodes = user.getAllSubscribedTypeCodes()

        /**
         * Save user in legacy format (for dual-write)
         */

        val notificationTypes = typeCodes.mapNotNull { typeCode ->
            try {
                LegacyNotificationType.valueOf(typeCode)
            } catch (e: IllegalArgumentException) {
                logger.warn { "Unknown type code: $typeCode, skipping in legacy save" }
                null
            }
        }.toMutableSet()

        val legacyEntity = LegacyUserEntity(
            id = user.id.value,
            notifications = notificationTypes,
        )

        legacyUserJpaRepository.save(legacyEntity)
        logger.debug { "Saved legacy format for user ${user.id.value}: ${typeCodes.joinToString(";")}" }
    }
}
