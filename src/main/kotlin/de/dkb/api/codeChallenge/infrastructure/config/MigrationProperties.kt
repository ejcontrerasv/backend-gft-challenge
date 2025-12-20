package de.dkb.api.codeChallenge.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for migration settings
 */
@Configuration
@ConfigurationProperties(prefix = "migration")
class MigrationProperties(var dualWrite: DualWriteConfig = DualWriteConfig(), var batchJob: BatchJobConfig = BatchJobConfig())

data class DualWriteConfig(var enabled: Boolean = true, var readSource: ReadSource = ReadSource.NEW_WITH_FALLBACK)

enum class ReadSource {
    /**
     * Read from the new schema first, fallback to legacy (default during migration)
     */
    NEW_WITH_FALLBACK,

    /**
     * Read-only from new schema (after migration complete)
     */
    NEW_ONLY,

    /**
     * Read-only from the legacy schema (rollback scenario)
     */
    LEGACY_ONLY,
}

data class BatchJobConfig(var enabled: Boolean = false, var batchSize: Int = 1000, var cron: String = "0 0 2 * * ?")
