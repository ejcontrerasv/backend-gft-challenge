package de.dkb.api.codeChallenge.application.dto

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import java.util.UUID

/**
 * Command to send a notification to a user
 */
data class SendNotificationCommand(
    val userId: UserId,
    val notificationType: String,
    val message: String
) {
    companion object {
        fun from(userId: UUID, type: String, message: String): SendNotificationCommand {
            return SendNotificationCommand(
                userId = UserId(userId),
                notificationType = type,
                message = message
            )
        }
    }
}
