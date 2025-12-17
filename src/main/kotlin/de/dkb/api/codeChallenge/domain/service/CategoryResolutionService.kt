package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.domain.model.NotificationCategory

/**
 * Domain service for resolving notification types to their categories.
 * Handles dynamic type-to-category mapping based on configuration.
 */
interface CategoryResolutionService {
    /**
     * Resolve which category a notification type belongs to
     */
    fun resolveCategoryByTypeCode(typeCode: String): NotificationCategory?

    /**
     * Resolve multiple categories from a set of legacy type codes
     * Used for migration: ["type1", "type5"] -> [CategoryA, CategoryB]
     */
    fun resolveCategoriesFromLegacyTypes(typeCodes: Set<String>): Set<NotificationCategory>

    /**
     * Check if a type code is valid (exists in any category)
     */
    fun isValidTypeCode(typeCode: String): Boolean
}

