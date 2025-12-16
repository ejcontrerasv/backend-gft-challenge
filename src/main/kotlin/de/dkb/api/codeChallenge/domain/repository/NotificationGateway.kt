package de.dkb.api.codeChallenge.domain.repository

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId

/**
 * Gateway interface (output port) for sending notifications.
 * Abstracts the actual notification delivery mechanism.
 * Implementation can be console logging, push service, email, etc.
 */
interface NotificationGateway {
    /**
     * Send a notification to a user
     *
     * @param userId The recipient user identifier
     * @param notificationType The type of notification being sent
     * @param message The notification message content
     */
    fun sendNotification(userId: UserId, notificationType: String, message: String)

    /**
     * Send bulk notifications to multiple users
     *
     * @param userIds List of recipient user identifiers
     * @param notificationType The type of notification being sent
     * @param message The notification message content
     */
    fun sendBulkNotification(userIds: List<UserId>, notificationType: String, message: String) {
        userIds.forEach { userId ->
            sendNotification(userId, notificationType, message)
        }
    }
}
