package de.dkb.api.codeChallenge.infrastructure.adapter.kafka.dto

import java.util.UUID

/**
 * Kafka message DTO for notification events.
 * Maintains compatibility with existing Kafka message format.
 */
data class NotificationKafkaMessage(val userId: UUID, val notificationType: String, val message: String)
