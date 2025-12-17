package de.dkb.api.codeChallenge.application.service

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.repository.CategoryConfigRepository
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Default implementation of CategoryResolutionService.
 * Located in application layer as it orchestrates domain logic with infrastructure dependencies.
 */
class DefaultCategoryResolutionService(private val categoryConfigRepository: CategoryConfigRepository) : CategoryResolutionService {

    override fun resolveCategoryByTypeCode(typeCode: String): NotificationCategory? {
        val normalizedCode = typeCode.trim().lowercase()

        return categoryConfigRepository.findCategoryByTypeCode(normalizedCode).also {
            if (it == null) {
                logger.warn { "Unknown notification type code: $typeCode" }
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
                logger.warn { "Skipping unknown type code during migration: $typeCode" }
            }
        }

        return categories
    }

    override fun isValidTypeCode(typeCode: String): Boolean = resolveCategoryByTypeCode(typeCode) != null
}
