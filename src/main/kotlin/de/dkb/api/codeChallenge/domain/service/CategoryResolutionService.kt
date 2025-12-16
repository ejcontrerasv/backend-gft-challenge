package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.repository.CategoryConfigRepository
import org.slf4j.LoggerFactory

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

/**
 * Default implementation of CategoryResolutionService
 */
class DefaultCategoryResolutionService(
    private val categoryConfigRepository: CategoryConfigRepository
) : CategoryResolutionService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun resolveCategoryByTypeCode(typeCode: String): NotificationCategory? {
        val normalizedCode = typeCode.trim().lowercase()

        return categoryConfigRepository.findCategoryByTypeCode(normalizedCode).also {
            if (it == null) {
                logger.warn("Unknown notification type code: $typeCode")
            }
        }
    }

    override fun resolveCategoriesFromLegacyTypes(typeCodes: Set<String>): Set<NotificationCategory> {
        val categories = mutableSetOf<NotificationCategory>()

        typeCodes.forEach { typeCode ->
            val category = resolveCategoryByTypeCode(typeCode)
            if (category != null) {
                categories.add(category)
            } else {
                logger.warn("Skipping unknown type code during migration: $typeCode")
            }
        }

        return categories
    }

    override fun isValidTypeCode(typeCode: String): Boolean {
        return resolveCategoryByTypeCode(typeCode) != null
    }
}
