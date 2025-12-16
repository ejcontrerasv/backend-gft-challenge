package de.dkb.api.codeChallenge.application.usecase

import de.dkb.api.codeChallenge.application.dto.NotificationResult
import de.dkb.api.codeChallenge.application.dto.SendNotificationCommand
import de.dkb.api.codeChallenge.domain.repository.NotificationGateway
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import org.slf4j.LoggerFactory

/**
 * Use case for sending notifications to users.
 * Validates user subscription before sending notification.
 */
class SendNotificationUseCase(
    private val userRepository: UserRepository,
    private val categoryResolutionService: CategoryResolutionService,
    private val notificationGateway: NotificationGateway
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: SendNotificationCommand): NotificationResult {
        logger.debug("Attempting to send notification type '${command.notificationType}' to user ${command.userId.value}")

        // Find user
        val user = userRepository.findById(command.userId)
        if (user == null) {
            logger.warn("User not found: ${command.userId.value}")
            return NotificationResult.UserNotFound("User ${command.userId.value} not found")
        }

        // Check if notification type is valid
        if (!categoryResolutionService.isValidTypeCode(command.notificationType)) {
            logger.warn("Invalid notification type: ${command.notificationType}")
            return NotificationResult.NotSent("Invalid notification type: ${command.notificationType}")
        }

        // Dynamic resolution: check if user's subscribed categories include this type
        if (!user.canReceiveNotificationType(command.notificationType)) {
            logger.info("User ${command.userId.value} not subscribed to type ${command.notificationType}")
            return NotificationResult.NotSent(
                "User not subscribed to notification type: ${command.notificationType}"
            )
        }

        // Send notification
        try {
            notificationGateway.sendNotification(
                userId = command.userId,
                notificationType = command.notificationType,
                message = command.message
            )

            logger.info("Notification sent successfully to user ${command.userId.value}")
            return NotificationResult.Sent("Notification sent successfully")

        } catch (e: Exception) {
            logger.error("Failed to send notification to user ${command.userId.value}", e)
            return NotificationResult.NotSent("Failed to send notification: ${e.message}")
        }
    }
}
