package de.dkb.api.codeChallenge.domain.model

import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class NotificationTypeTest {

    @Test
    fun `should create notification type`() {
        val now = Instant.now()
        val categoryId = CategoryId("CATEGORY_A")
        val type = NotificationType(
            code = "type1",
            categoryId = categoryId,
            addedAt = now,
        )

        assertEquals("type1", type.code)
        assertEquals(categoryId, type.categoryId)
        assertEquals(now, type.addedAt)
    }

    @Test
    fun `should create notification type with default addedAt`() {
        val type = NotificationType(
            code = "type1",
            categoryId = CategoryId("CATEGORY_A"),
        )

        assertNotNull(type.addedAt)
    }

    @Test
    fun `should have correct equality`() {
        val now = Instant.now()
        val categoryId = CategoryId("CATEGORY_A")
        val type1 = NotificationType(code = "type1", categoryId = categoryId, addedAt = now)
        val type2 = NotificationType(code = "type1", categoryId = categoryId, addedAt = now)

        assertEquals(type1, type2)
        assertEquals(type1.hashCode(), type2.hashCode())
    }

    @Test
    fun `should be different for different codes`() {
        val categoryId = CategoryId("CATEGORY_A")
        val type1 = NotificationType(code = "type1", categoryId = categoryId)
        val type2 = NotificationType(code = "type2", categoryId = categoryId)

        assertNotEquals(type1, type2)
    }

    @Test
    fun `should throw exception for blank code`() {
        assertThrows<IllegalArgumentException> {
            NotificationType(code = "", categoryId = CategoryId("CATEGORY_A"))
        }
    }

    @Test
    fun `should create with factory method`() {
        val type = NotificationType.withCode("type1")

        assertEquals("type1", type.code)
        assertEquals(CategoryId("UNKNOWN"), type.categoryId)
    }
}
