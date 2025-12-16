package de.dkb.api.codeChallenge.infrastructure.persistence.adapter

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.repository.CategoryConfigRepository
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.NotificationCategoryJpaRepository
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.NotificationTypeJpaRepository
import de.dkb.api.codeChallenge.infrastructure.persistence.mapper.NotificationCategoryMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Database-driven implementation of CategoryConfigRepository.
 * Provides access to notification categories and types with caching.
 */
@Component
class CategoryConfigRepositoryAdapter(
    private val categoryJpaRepository: NotificationCategoryJpaRepository,
    private val typeJpaRepository: NotificationTypeJpaRepository
) : CategoryConfigRepository {

    private val cache = ConcurrentHashMap<CategoryId, NotificationCategory>()
    private val typeToCategory = ConcurrentHashMap<String, NotificationCategory>()

    init {
        refresh()
    }

    override fun findAllCategories(): List<NotificationCategory> {
        if (cache.isEmpty()) refresh()
        return cache.values.toList()
    }

    override fun findCategoryById(id: CategoryId): NotificationCategory? {
        if (cache.isEmpty()) refresh()
        return cache[id]
    }

    override fun findTypesByCategory(categoryId: CategoryId): Set<NotificationType> {
        return findCategoryById(categoryId)?.types ?: emptySet()
    }

    override fun findCategoryByTypeCode(typeCode: String): NotificationCategory? {
        if (typeToCategory.isEmpty()) refresh()
        return typeToCategory[typeCode.trim().lowercase()]
    }

    override fun getAllTypeCodes(): Set<String> {
        if (typeToCategory.isEmpty()) refresh()
        return typeToCategory.keys
    }

    override fun refresh() {
        logger.info { "Refreshing category configuration cache" }

        try {
            val categoryEntities = categoryJpaRepository.findAllWithTypes()
            val categories = NotificationCategoryMapper.toDomainList(categoryEntities)

            cache.clear()
            typeToCategory.clear()

            categories.forEach { category ->
                cache[category.id] = category
                category.types.forEach { type ->
                    typeToCategory[type.code.lowercase()] = category
                }
            }

            logger.info { "Configuration cache refreshed: ${cache.size} categories, ${typeToCategory.size} types" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to refresh category configuration" }
        }
    }
}
