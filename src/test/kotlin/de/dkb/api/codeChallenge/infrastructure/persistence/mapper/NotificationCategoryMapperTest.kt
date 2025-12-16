package de.dkb.api.codeChallenge.infrastructure.persistence.mapper

import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.NotificationCategoryEntity
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.NotificationTypeEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class NotificationCategoryMapperTest {

    @Test
    fun `should map entity to domain`() {
        val now = Instant.now()
        val categoryEntity = NotificationCategoryEntity(
            id = "CATEGORY_A",
            name = "Category A",
            createdAt = now,
            updatedAt = now,
        )
        val typeEntity = NotificationTypeEntity(
            code = "type1",
            category = categoryEntity,
            addedAt = now,
        )
        categoryEntity.types.add(typeEntity)

        val domain = NotificationCategoryMapper.toDomain(categoryEntity)

        assertEquals("CATEGORY_A", domain.id.value)
        assertEquals("Category A", domain.name)
        assertEquals(1, domain.types.size)
    }

    @Test
    fun `should map entity list to domain list`() {
        val now = Instant.now()
        val entity1 = NotificationCategoryEntity(
            id = "CATEGORY_A",
            name = "Category A",
            createdAt = now,
            updatedAt = now,
        )
        val entity2 = NotificationCategoryEntity(
            id = "CATEGORY_B",
            name = "Category B",
            createdAt = now,
            updatedAt = now,
        )

        val domains = NotificationCategoryMapper.toDomainList(listOf(entity1, entity2))

        assertEquals(2, domains.size)
    }

    @Test
    fun `should handle empty types`() {
        val now = Instant.now()
        val entity = NotificationCategoryEntity(
            id = "CATEGORY_A",
            name = "Category A",
            createdAt = now,
            updatedAt = now,
        )

        val domain = NotificationCategoryMapper.toDomain(entity)

        assertTrue(domain.types.isEmpty())
    }
}
