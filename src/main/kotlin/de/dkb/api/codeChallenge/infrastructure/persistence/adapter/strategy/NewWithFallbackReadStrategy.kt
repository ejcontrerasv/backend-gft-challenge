package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
import de.dkb.api.codeChallenge.infrastructure.persistence.adapter.UserRepositoryAdapter
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.LegacyUserEntity
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.LegacyUserJpaRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class NewWithFallbackReadStrategy(
    private val newUserRepository: UserRepositoryAdapter,
    private val legacyUserJpaRepository: LegacyUserJpaRepository,
    private val legacyDataMigrator: LegacyDataMigrator,
) : UserReadStrategy {

    override val type: ReadSource = ReadSource.NEW_WITH_FALLBACK

    override fun findById(id: UserId): User? {
        val userFromNew = newUserRepository.findById(id)
        if (userFromNew != null) {
            logger.debug { "User ${id.value} found in new schema" }
            return userFromNew
        }

        logger.debug { "User ${id.value} not in new schema, checking legacy" }
        val legacyUser = legacyUserJpaRepository.findById(id.value).orElse(null)
        return if (legacyUser != null) {
            logger.info { "Performing on-the-fly migration for user ${id.value}" }
            migrateUserFromLegacy(legacyUser)
        } else {
            null
        }
    }

    override fun existsById(id: UserId): Boolean =
        newUserRepository.existsById(id) || legacyUserJpaRepository.existsById(id.value)

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
        val legacyTypesString = legacyUser.getNotificationsAsString()
        val subscriptions = legacyDataMigrator.migrateUserTypes(
            userId = UserId(legacyUser.id),
            legacyTypes = legacyTypesString,
        )
        if (subscriptions.isEmpty()) {
            logger.warn { "Migration produced no subscriptions for user ${legacyUser.id}" }
            return null
        }
        val migratedUser = User(
            id = UserId(legacyUser.id),
            subscriptions = subscriptions,
        )
        newUserRepository.save(migratedUser)
        logger.info { "On-the-fly migration successful for user ${legacyUser.id}" }
        return migratedUser
    }
}
