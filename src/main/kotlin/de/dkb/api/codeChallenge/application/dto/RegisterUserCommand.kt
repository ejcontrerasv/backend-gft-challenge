package de.dkb.api.codeChallenge.application.dto

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import java.util.UUID

/**
 * Command to register a user with notification subscriptions.
 * Accepts legacy type-based format for backward compatibility.
 */
data class RegisterUserCommand(
    val userId: UserId,
    val notificationTypes: Set<String>
) {
    companion object {
        fun from(userId: UUID, types: Set<String>): RegisterUserCommand {
            return RegisterUserCommand(
                userId = UserId(userId),
                notificationTypes = types
            )
        }
    }
}
