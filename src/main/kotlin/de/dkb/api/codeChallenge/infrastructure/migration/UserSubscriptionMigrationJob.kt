package de.dkb.api.codeChallenge.infrastructure.migration

import de.dkb.api.codeChallenge.application.dto.MigrateUserCommand
import de.dkb.api.codeChallenge.application.usecase.MigrateUserSubscriptionsUseCase
import de.dkb.api.codeChallenge.application.usecase.MigrationResult
import de.dkb.api.codeChallenge.infrastructure.config.MigrationProperties
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.LegacyUserJpaRepository
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.UserSubscriptionJpaRepository
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Scheduled job for batch migration of legacy users to new schema.
 * Runs nightly to gradually migrate users without impacting system performance.
 */
@Component
@ConditionalOnProperty(
    name = ["migration.batch-job.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class UserSubscriptionMigrationJob(
    private val legacyUserJpaRepository: LegacyUserJpaRepository,
    private val userSubscriptionJpaRepository: UserSubscriptionJpaRepository,
    private val migrateUserSubscriptionsUseCase: MigrateUserSubscriptionsUseCase,
    private val migrationProperties: MigrationProperties
) {

    @Scheduled(cron = "\${migration.batch-job.cron}")
    fun migrateBatch() {
        val startTime = Instant.now()
        logger.info { "=== Starting batch migration job ===" }

        try {
            val batchSize = migrationProperties.batchJob.batchSize
            var page = 0
            var totalMigrated = 0
            var totalFailed = 0
            var hasMore = true

            while (hasMore) {
                val pageable = PageRequest.of(page, batchSize)
                val legacyUsers = legacyUserJpaRepository.findAll(pageable)

                if (legacyUsers.isEmpty) {
                    hasMore = false
                    continue
                }

                logger.info { "Processing batch $page: ${legacyUsers.numberOfElements} users" }

                val usersToMigrate = legacyUsers.content.filter { legacyUser ->
                    !userSubscriptionJpaRepository.findByUserId(legacyUser.id).any()
                }

                if (usersToMigrate.isEmpty()) {
                    logger.info { "All users in batch $page already migrated, skipping" }
                    page++
                    hasMore = legacyUsers.hasNext()
                    continue
                }

                logger.info { "Found ${usersToMigrate.size} users to migrate in this batch" }

                usersToMigrate.forEach { legacyUser ->
                    val command = MigrateUserCommand.from(
                        userId = legacyUser.id,
                        legacyTypes = legacyUser.getNotificationsAsString()
                    )

                    when (val result = migrateUserSubscriptionsUseCase.execute(command)) {
                        is MigrationResult.Success -> {
                            totalMigrated++
                            logger.debug { "✓ Migrated user ${legacyUser.id}" }
                        }
                        is MigrationResult.Failed -> {
                            totalFailed++
                            logger.warn { "✗ Failed to migrate user ${legacyUser.id}: ${result.reason}" }
                        }
                    }
                }

                page++
                hasMore = legacyUsers.hasNext()
                logger.info { "Batch $page completed: $totalMigrated migrated, $totalFailed failed so far" }
            }

            val duration = Duration.between(startTime, Instant.now())
            logger.info { "=== Batch migration job completed ===" }
            logger.info { "Total migrated: $totalMigrated" }
            logger.info { "Total failed: $totalFailed" }
            logger.info { "Duration: ${duration.toMinutes()} minutes ${duration.toSecondsPart()} seconds" }

            checkMigrationProgress()

        } catch (e: Exception) {
            logger.error(e) { "Batch migration job failed with exception" }
        }
    }

    /**
     * Check overall migration progress
     */
    private fun checkMigrationProgress() {
        val totalLegacyUsers = legacyUserJpaRepository.count()
        val migratedUserIds = userSubscriptionJpaRepository.findAll().map { it.userId }.distinct()
        val totalMigrated = migratedUserIds.size

        val percentage = if (totalLegacyUsers > 0) {
            (totalMigrated.toDouble() / totalLegacyUsers.toDouble() * 100).toInt()
        } else {
            100
        }

        logger.info { "=== Migration Progress ===" }
        logger.info { "Total legacy users: $totalLegacyUsers" }
        logger.info { "Total migrated: $totalMigrated" }
        logger.info { "Progress: $percentage%" }

        if (percentage >= 100) {
            logger.info { "🎉 Migration is 100% complete!" }
        }
    }

    /**
     * Manual trigger for testing
     */
    fun triggerManualMigration(): String {
        logger.info { "Manual migration triggered" }
        migrateBatch()
        return "Migration batch completed. Check logs for details."
    }
}
