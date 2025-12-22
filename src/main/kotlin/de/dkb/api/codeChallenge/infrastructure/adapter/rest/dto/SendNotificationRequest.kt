package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * API request DTO for sending notifications.
 * Maintains backward compatibility with existing API contract.
 */
@Schema(description = "Request to send a notification to a user")
data class SendNotificationRequest(
    @field:NotNull(message = "User ID is required")
    @field:Schema(description = "User identifier to send notification to", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,

    @field:NotBlank(message = "Notification type is required")
    @field:Size(min = 1, max = 50, message = "Notification type must be between 1 and 50 characters")
    @field:Schema(description = "Type of notification to send", example = "type1")
    val notificationType: String,

    @field:NotBlank(message = "Message is required")
    @field:Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
    @field:Schema(description = "Notification message content", example = "Your order has shipped!")
    val message: String,
)
