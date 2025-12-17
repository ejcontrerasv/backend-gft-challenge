package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * API request DTO for sending notifications.
 * Maintains backward compatibility with existing API contract.
 */
@Schema(description = "Request to send a notification to a user")
data class SendNotificationRequest(
    @field:Schema(description = "User identifier to send notification to", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,

    @field:Schema(description = "Type of notification to send", example = "type1")
    val notificationType: String,

    @field:Schema(description = "Notification message content", example = "Your order has shipped!")
    val message: String,
)
