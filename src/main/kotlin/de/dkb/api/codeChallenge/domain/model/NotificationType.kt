package de.dkb.api.codeChallenge.domain.model

import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import java.time.Instant

/**
 * Domain entity representing a notification type (e.g., type1, type2).
 * Each type belongs to a specific category.
 */
data class NotificationType(
    val code: String,
    val categoryId: CategoryId,
    val addedAt: Instant = Instant.now()
) {
    init {
        require(code.isNotBlank()) { "Notification type code cannot be blank" }
    }

    companion object {
        /**
         * Create a NotificationType with unknown category (for resolution later)
         */
        fun withCode(code: String): NotificationType =
            NotificationType(code, CategoryId("UNKNOWN"), Instant.now())
    }
}
