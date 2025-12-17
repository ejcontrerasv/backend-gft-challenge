package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

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
data class RegisterUserResponse(
    val userId: String,
    val subscribedCategories: List<String>
)

/**
 * Notification response
 */
data class SendNotificationResponse(
    val sent: Boolean,
    val message: String
)
