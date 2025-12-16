package de.dkb.api.codeChallenge.domain.repository

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId

/**
 * Repository interface (output port) for notification category configuration.
 * Provides access to categories and their associated notification types.
 * Implementation can be database-driven or configuration-based.
 */
interface CategoryConfigRepository {
    /**
     * Find all notification categories with their types
     */
    fun findAllCategories(): List<NotificationCategory>

    /**
     * Find a specific category by its identifier
     */
    fun findCategoryById(id: CategoryId): NotificationCategory?

    /**
     * Find all notification types belonging to a category
     */
    fun findTypesByCategory(categoryId: CategoryId): Set<NotificationType>

    /**
     * Find category that contains a specific notification type code
     */
    fun findCategoryByTypeCode(typeCode: String): NotificationCategory?

    /**
     * Get all notification type codes across all categories
     */
    fun getAllTypeCodes(): Set<String>

    /**
     * Refresh/reload category configuration (for dynamic updates)
     */
    fun refresh()
}
