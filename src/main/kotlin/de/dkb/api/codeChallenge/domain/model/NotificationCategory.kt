package de.dkb.api.codeChallenge.domain.model

import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId

/**
 * Domain entity representing a notification category (e.g., Category A, Category B).
 * Aggregate root for notification types within the category.
 */
data class NotificationCategory(
    val id: CategoryId,
    val name: String,
    val types: Set<NotificationType> = emptySet()
) {
    init {
        require(name.isNotBlank()) { "Category name cannot be blank" }
    }

    /**
     * Check if this category contains a specific notification type
     */
    fun containsType(typeCode: String): Boolean =
        types.any { it.code == typeCode }

    /**
     * Get all type codes in this category
     */
    fun getTypeCodes(): Set<String> =
        types.map { it.code }.toSet()
}
