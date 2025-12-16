package de.dkb.api.codeChallenge.infrastructure.persistence.adapter

import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
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
 *
 * Pattern:
 * - WRITE: Writes to both new (user_subscriptions) and legacy (users_legacy) tables
 * - READ: Reads from new table first, fallback to legacy with on-the-fly migration
 *
 * This enables:
 * - Zero-downtime migration
 * - Instant rollback capability (switch read source via config)
 * - Lazy migration (users migrated on first access)
 * - Data consistency during transition period
 */
@Component
@Primary
@ConditionalOnProperty(
    name = ["migration.dual-write.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class DualWriteUserRepositoryAdapter(
    private val newUserRepository: UserRepositoryAdapter,
    private val legacyUserJpaRepository: LegacyUserJpaRepository,
    private val legacyDataMigrator: LegacyDataMigrator
) : UserRepository {

    /**
     * Read: Try new table first, fallback to legacy with on-the-fly migration
     */
    override fun findById(id: UserId): User? {
        val userFromNew = newUserRepository.findById(id)
        if (userFromNew != null) {
            logger.debug { "User ${id.value} found in new schema" }
            return userFromNew
        }

        logger.debug { "User ${id.value} not in new schema, checking legacy" }
        val legacyUser = legacyUserJpaRepository.findById(id.value).orElse(null) ?: return null

        logger.info { "Performing on-the-fly migration for user ${id.value}" }
        return migrateUserFromLegacy(legacyUser)
    }

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

    override fun findByCategory(categoryId: CategoryId): List<User> {
        return newUserRepository.findByCategory(categoryId)
    }

    override fun existsById(id: UserId): Boolean {
        return newUserRepository.existsById(id) || legacyUserJpaRepository.existsById(id.value)
    }

    override fun count(): Long {
        val newUsers = newUserRepository.findAll().map { it.id.value }.toSet()
        val legacyUsers = legacyUserJpaRepository.findAll().map { it.id }.toSet()
        return (newUsers + legacyUsers).size.toLong()
    }

    override fun findAll(): List<User> {
        val newUsers = newUserRepository.findAll()
        val newUserIds = newUsers.map { it.id.value }.toSet()

        val legacyUsers = legacyUserJpaRepository.findAll()
            .filter { it.id !in newUserIds }
            .mapNotNull { legacy ->
                try {
                    migrateUserFromLegacy(legacy)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to migrate legacy user ${legacy.id} during findAll" }
                    null
                }
            }

        return newUsers + legacyUsers
    }

    private fun migrateUserFromLegacy(legacyUser: LegacyUserEntity): User? {
        return try {
    /**
     * Migrate a legacy user on-the-fly
     */
            val legacyTypesString = legacyUser.getNotificationsAsString()
            val subscriptions = legacyDataMigrator.migrateUserTypes(
                userId = UserId(legacyUser.id),
                legacyTypes = legacyTypesString
            )

            if (subscriptions.isEmpty()) {
                logger.warn { "Migration produced no subscriptions for user ${legacyUser.id}" }
                return null
            }

            val migratedUser = User(
                id = UserId(legacyUser.id),
                subscriptions = subscriptions
            )

            newUserRepository.save(migratedUser)
            logger.info { "On-the-fly migration successful for user ${legacyUser.id}" }
            migratedUser
        } catch (e: Exception) {
            logger.error(e) { "On-the-fly migration failed for user ${legacyUser.id}" }
            null
        }
    }

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
            notifications = notificationTypes
        )

        legacyUserJpaRepository.save(legacyEntity)
        logger.debug { "Saved legacy format for user ${user.id.value}: ${typeCodes.joinToString(";")}" }
    }
}
