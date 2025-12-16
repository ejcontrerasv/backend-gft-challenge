package de.dkb.api.codeChallenge.application.usecase

import de.dkb.api.codeChallenge.application.dto.MigrateUserCommand
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case for migrating legacy user subscriptions to category-based model.
 * Used for batch migration and on-the-fly lazy migration.
 */
class MigrateUserSubscriptionsUseCase(
    private val userRepository: UserRepository,
    private val legacyDataMigrator: LegacyDataMigrator
) {

    /**
     * Migrate a single user's legacy subscriptions
     */
    fun execute(command: MigrateUserCommand): MigrationResult {
        logger.info { "Migrating user ${command.userId.value}: '${command.legacyNotificationTypes}'" }

        return try {
            // Migrate legacy types to category subscriptions
            val subscriptions = legacyDataMigrator.migrateUserTypes(
                userId = command.userId,
                legacyTypes = command.legacyNotificationTypes
            )

            if (subscriptions.isEmpty()) {
                logger.warn { "Migration produced no subscriptions for user ${command.userId.value}" }
                return MigrationResult.Failed(
                    userId = command.userId.value.toString(),
                    reason = "No valid subscriptions found in legacy data"
                )
            }

            // Create user with migrated subscriptions
            val user = User(
                id = command.userId,
                subscriptions = subscriptions
            )

            // Save to new schema
            userRepository.save(user)

            logger.info { "Successfully migrated user ${command.userId.value}: ${subscriptions.size} categories" }

            MigrationResult.Success(
                userId = command.userId.value.toString(),
                categoriesCount = subscriptions.size
            )

        } catch (e: Exception) {
            logger.error(e) { "Migration failed for user ${command.userId.value}" }
            MigrationResult.Failed(
                userId = command.userId.value.toString(),
                reason = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Migrate multiple users in batch
     */
    fun executeBatch(commands: List<MigrateUserCommand>): BatchMigrationResult {
        logger.info { "Starting batch migration for ${commands.size} users" }

        val results = commands.map { execute(it) }

        val successCount = results.count { it is MigrationResult.Success }
        val failedCount = results.count { it is MigrationResult.Failed }

        logger.info { "Batch migration completed: $successCount succeeded, $failedCount failed" }

        return BatchMigrationResult(
            total = commands.size,
            succeeded = successCount,
            failed = failedCount,
            results = results
        )
    }
}

/**
 * Result of a single user migration
 */
sealed class MigrationResult {
    data class Success(
        val userId: String,
        val categoriesCount: Int
    ) : MigrationResult()

    data class Failed(
        val userId: String,
        val reason: String
    ) : MigrationResult()
}

/**
 * Result of batch migration
 */
data class BatchMigrationResult(
    val total: Int,
    val succeeded: Int,
    val failed: Int,
    val results: List<MigrationResult>
)
