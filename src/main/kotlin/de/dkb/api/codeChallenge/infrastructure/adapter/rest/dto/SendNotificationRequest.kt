package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import java.util.UUID

/**
 * API request DTO for sending notifications.
 * Maintains backward compatibility with existing API contract.
 */
data class SendNotificationRequest(
    val userId: UUID,
    val notificationType: String,
    val message: String
)
