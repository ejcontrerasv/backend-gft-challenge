package de.dkb.api.codeChallenge.application.dto

/**
 * Result of notification sending operation
 */
sealed class NotificationResult {
    data class Sent(val message: String) : NotificationResult()
    data class NotSent(val reason: String) : NotificationResult()
    data class UserNotFound(val message: String) : NotificationResult()
}
