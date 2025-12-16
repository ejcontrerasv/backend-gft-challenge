package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Generic API response wrapper
 */
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T, val message: String? = null) : ApiResponse<T>()
    data class Error(val message: String, val errors: List<String> = emptyList()) : ApiResponse<Nothing>()
}

/**
 * Registration response
 */
@Schema(description = "Response after successful user registration")
data class RegisterUserResponse(
    @Schema(description = "Registered user ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: String,

    @Schema(description = "List of subscribed categories", example = "[\"CATEGORY_A\", \"CATEGORY_B\"]")
    val subscribedCategories: List<String>,
)

/**
 * Notification response
 */
@Schema(description = "Response after notification processing")
data class SendNotificationResponse(
    @Schema(description = "Whether the notification was sent", example = "true")
    val sent: Boolean,

    @Schema(description = "Result message", example = "Notification sent successfully")
    val message: String,
)
