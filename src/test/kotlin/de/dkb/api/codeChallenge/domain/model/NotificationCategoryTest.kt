package de.dkb.api.codeChallenge.domain.model

import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class NotificationCategoryTest {

    @Test
    fun `should create category with types`() {
        val categoryId = CategoryId("CATEGORY_A")
        val types = setOf(
            NotificationType("type1", categoryId, Instant.now()),
            NotificationType("type2", categoryId, Instant.now()),
        )

        val category = NotificationCategory(
            id = categoryId,
            name = "Category A",
            types = types,
        )

        assertEquals("CATEGORY_A", category.id.value)
        assertEquals("Category A", category.name)
        assertEquals(2, category.types.size)
    }

    @Test
    fun `should check if category contains type`() {
        val categoryId = CategoryId("CATEGORY_A")
        val types = setOf(
            NotificationType("type1", categoryId, Instant.now()),
            NotificationType("type2", categoryId, Instant.now()),
        )
        val category = NotificationCategory(id = categoryId, name = "Category A", types = types)

        assertTrue(category.containsType("type1"))
        assertTrue(category.containsType("type2"))
        assertFalse(category.containsType("type3"))
    }

    @Test
    fun `should get all type codes`() {
        val categoryId = CategoryId("CATEGORY_A")
        val types = setOf(
            NotificationType("type1", categoryId, Instant.now()),
            NotificationType("type2", categoryId, Instant.now()),
            NotificationType("type3", categoryId, Instant.now()),
        )
        val category = NotificationCategory(id = categoryId, name = "Category A", types = types)

        val typeCodes = category.getTypeCodes()

        assertEquals(3, typeCodes.size)
        assertTrue(typeCodes.containsAll(setOf("type1", "type2", "type3")))
    }

    @Test
    fun `should handle empty types`() {
        val category = NotificationCategory(
            id = CategoryId("EMPTY"),
            name = "Empty Category",
            types = emptySet(),
        )

        assertFalse(category.containsType("any"))
        assertTrue(category.getTypeCodes().isEmpty())
    }
}
