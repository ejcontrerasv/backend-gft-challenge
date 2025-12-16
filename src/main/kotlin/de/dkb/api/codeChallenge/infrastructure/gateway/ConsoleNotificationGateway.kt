package de.dkb.api.codeChallenge.infrastructure.gateway

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.repository.NotificationGateway
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Console-based implementation of NotificationGateway.
 * Simulates notification sending by logging to console.
 * In production, this would be replaced with actual push notification service.
 */
@Component
class ConsoleNotificationGateway : NotificationGateway {

    override fun sendNotification(userId: UserId, notificationType: String, message: String) {
        val formattedMessage = """
            ╔════════════════════════════════════════════════════════════╗
            ║ NOTIFICATION SENT                                          ║
            ╠════════════════════════════════════════════════════════════╣
            ║ User ID: ${userId.value.toString().padEnd(45)}     ║
            ║ Type:    ${notificationType.padEnd(45)}            ║
            ║ Message: ${message.take(45).padEnd(45)}        ║
            ╚════════════════════════════════════════════════════════════╝
        """.trimIndent()

        logger.info { formattedMessage }
    }

    override fun sendBulkNotification(userIds: List<UserId>, notificationType: String, message: String) {
        logger.info { "Sending bulk notification of type '$notificationType' to ${userIds.size} users" }
        userIds.forEach { sendNotification(it, notificationType, message) }
        logger.info { "Bulk notification completed for ${userIds.size} users" }
    }
}
