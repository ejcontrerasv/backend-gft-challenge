package de.dkb.api.codeChallenge.domain.model

import java.time.Instant

/**
 * Domain entity representing a user's subscription to a notification category.
 * Part of the User aggregate.
 */
data class CategorySubscription(val category: NotificationCategory, val subscribedAt: Instant, val active: Boolean = true) {
    /**
     * Check if this subscription is currently active
     */
    fun isActive(): Boolean = active

    /**
     * Check if this subscription belongs to a specific category
     */
    fun belongsToCategory(categoryId: String): Boolean =
        category.id.value == categoryId
}
