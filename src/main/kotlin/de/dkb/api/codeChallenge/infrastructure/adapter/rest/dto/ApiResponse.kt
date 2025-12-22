package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Generic API response wrapper
 */
sealed class ApiResponse<out T> {
    data class Success<T>(
        @field:Schema(description = "Payload for successful responses")
        val data: T,
        @field:Schema(description = "Optional message", example = "Operation completed")
        val message: String? = null,
    ) : ApiResponse<T>()

    data class Error<T>(
        @field:Schema(description = "Error message", example = "Validation failed")
        val message: String,
        @field:Schema(description = "List of validation errors", example = "['Field A is required']")
        val errors: List<String> = emptyList(),
    ) : ApiResponse<T>()

    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> = Success(data, message)
        fun <T> error(message: String, errors: List<String> = emptyList()): ApiResponse<T> = Error(message, errors)
    }
}

/**
 * Registration response
 */
@Schema(description = "Response after successful user registration")
data class RegisterUserResponse(
    @field:Schema(description = "Registered user ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: String,

    @field:Schema(description = "List of subscribed categories", example = "['CATEGORY_A', 'CATEGORY_B']")
    val subscribedCategories: List<String>,
)

/**
 * Notification response
 */
@Schema(description = "Response after notification processing")
data class SendNotificationResponse(
    @field:Schema(description = "Whether the notification was sent", example = "true")
    val sent: Boolean,

    @field:Schema(description = "Result message", example = "Notification sent successfully")
    val message: String,
)
