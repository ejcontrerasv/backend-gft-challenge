package de.dkb.api.codeChallenge.application.dto

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import java.util.UUID

/**
 * Command to migrate a legacy user's type-based subscriptions
 * to category-based model
 */
data class MigrateUserCommand(val userId: UserId, val legacyNotificationTypes: String) {
    companion object {
        fun from(userId: UUID, legacyTypes: String): MigrateUserCommand = MigrateUserCommand(
            userId = UserId(userId),
            legacyNotificationTypes = legacyTypes,
        )
    }
}
