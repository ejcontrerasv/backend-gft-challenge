package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class JpaEntitiesTest {

    @Test
    fun `NotificationCategoryEntity should create correctly`() {
        val now = Instant.now()
        val entity = NotificationCategoryEntity(
            id = "CATEGORY_A",
            name = "Category A",
            createdAt = now,
            updatedAt = now,
        )

        assertEquals("CATEGORY_A", entity.id)
        assertEquals("Category A", entity.name)
        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
        assertTrue(entity.types.isEmpty())
    }

    @Test
    fun `NotificationTypeEntity should create correctly`() {
        val now = Instant.now()
        val category = NotificationCategoryEntity(
            id = "CATEGORY_A",
            name = "Category A",
            createdAt = now,
            updatedAt = now,
        )
        val entity = NotificationTypeEntity(
            code = "type1",
            category = category,
            addedAt = now,
            active = true,
        )

        assertEquals("type1", entity.code)
        assertEquals(category, entity.category)
        assertEquals(now, entity.addedAt)
        assertTrue(entity.active)
    }

    @Test
    fun `UserSubscriptionEntity should create correctly`() {
        val userId = UUID.randomUUID()
        val now = Instant.now()
        val entity = UserSubscriptionEntity(
            userId = userId,
            categoryId = "CATEGORY_A",
            subscribedAt = now,
            active = true,
        )

        assertEquals(userId, entity.userId)
        assertEquals("CATEGORY_A", entity.categoryId)
        assertEquals(now, entity.subscribedAt)
        assertTrue(entity.active)
    }

    @Test
    fun `LegacyUserEntity should create correctly`() {
        val userId = UUID.randomUUID()
        val notifications = mutableSetOf(LegacyNotificationType.type1, LegacyNotificationType.type2)
        val entity = LegacyUserEntity(
            id = userId,
            notifications = notifications,
        )

        assertEquals(userId, entity.id)
        assertEquals(2, entity.notifications.size)
    }

    @Test
    fun `LegacyUserEntity getNotificationsAsString should return notifications`() {
        val entity = LegacyUserEntity(
            id = UUID.randomUUID(),
            notifications = mutableSetOf(LegacyNotificationType.type1, LegacyNotificationType.type2),
        )

        val result = entity.getNotificationsAsString()

        assertTrue(result.contains("type1") || result.contains("type2"))
    }

    @Test
    fun `NotificationCategoryEntity should add types`() {
        val now = Instant.now()
        val category = NotificationCategoryEntity(
            id = "CATEGORY_A",
            name = "Category A",
            createdAt = now,
            updatedAt = now,
        )
        val type = NotificationTypeEntity(
            code = "type1",
            category = category,
            addedAt = now,
        )

        category.types.add(type)

        assertEquals(1, category.types.size)
    }

    @Test
    fun `NotificationTypeEntity equals should work correctly`() {
        val now = Instant.now()
        val type1 = NotificationTypeEntity(code = "type1", addedAt = now)
        val type2 = NotificationTypeEntity(code = "type1", addedAt = now)

        assertEquals(type1, type2)
        assertEquals(type1.hashCode(), type2.hashCode())
    }

    @Test
    fun `NotificationTypeEntity toString should return string representation`() {
        val type = NotificationTypeEntity(code = "type1", active = true)

        val result = type.toString()

        assertTrue(result.contains("type1"))
    }
}
