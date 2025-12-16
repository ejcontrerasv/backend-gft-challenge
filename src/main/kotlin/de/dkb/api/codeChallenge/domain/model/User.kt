package de.dkb.api.codeChallenge.domain.model

import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId

/**
 * Domain aggregate root representing a user with notification subscriptions.
 * Follows DDD patterns with category-based subscription model.
 */
data class User(val id: UserId, val subscriptions: Set<CategorySubscription> = emptySet()) {
    /**
     * Check if user is subscribed to a specific category
     */
    fun isSubscribedToCategory(categoryId: CategoryId): Boolean = subscriptions.any { it.category.id == categoryId && it.active }

    /**
     * Check if user is subscribed to a specific category by string ID
     */
    fun isSubscribedToCategory(categoryId: String): Boolean = isSubscribedToCategory(CategoryId(categoryId))

    /**
     * Check if user can receive a notification of specific type.
     * User can receive if they're subscribed to the category containing that type.
     */
    fun canReceiveNotificationType(typeCode: String): Boolean = subscriptions
        .filter { it.active }
        .any { it.category.containsType(typeCode) }

    /**
     * Get all active category IDs the user is subscribed to
     */
    fun getActiveCategoryIds(): Set<CategoryId> = subscriptions
        .filter { it.active }
        .map { it.category.id }
        .toSet()

    /**
     * Get all notification type codes the user is subscribed to (across all categories)
     */
    fun getAllSubscribedTypeCodes(): Set<String> = subscriptions
        .filter { it.active }
        .flatMap { it.category.getTypeCodes() }
        .toSet()

    /**
     * Add a new category subscription
     */
    fun addSubscription(subscription: CategorySubscription): User = copy(subscriptions = subscriptions + subscription)

    /**
     * Add multiple category subscriptions
     */
    fun addSubscriptions(newSubscriptions: Set<CategorySubscription>): User = copy(subscriptions = subscriptions + newSubscriptions)
}
