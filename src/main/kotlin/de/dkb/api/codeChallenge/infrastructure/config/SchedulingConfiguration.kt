package de.dkb.api.codeChallenge.infrastructure.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Configuration for Spring Scheduling.
 * Enabled only when migration batch job is active.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = ["migration.batch-job.enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class SchedulingConfiguration
