package de.dkb.api.codeChallenge.domain.model.valueobject

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ValueObjectsTest {

    @Test
    fun `UserId should wrap UUID correctly`() {
        val uuid = UUID.randomUUID()
        val userId = UserId(uuid)

        assertEquals(uuid, userId.value)
    }

    @Test
    fun `UserId should have correct equality`() {
        val uuid = UUID.randomUUID()
        val userId1 = UserId(uuid)
        val userId2 = UserId(uuid)

        assertEquals(userId1, userId2)
        assertEquals(userId1.hashCode(), userId2.hashCode())
    }

    @Test
    fun `UserId should be different for different UUIDs`() {
        val userId1 = UserId(UUID.randomUUID())
        val userId2 = UserId(UUID.randomUUID())

        assertNotEquals(userId1, userId2)
    }

    @Test
    fun `UserId toString should contain UUID`() {
        val uuid = UUID.randomUUID()
        val userId = UserId(uuid)

        assertTrue(userId.toString().contains(uuid.toString()))
    }

    @Test
    fun `UserId fromString should parse UUID`() {
        val uuid = UUID.randomUUID().toString()
        val userId = UserId.fromString(uuid)

        assertEquals(UUID.fromString(uuid), userId.value)
    }

    @Test
    fun `UserId random should generate non-null distinct values`() {
        val a = UserId.random()
        val b = UserId.random()
        assertNotNull(a.value)
        assertNotNull(b.value)
        assertNotEquals(a, b)
    }

    @Test
    fun `CategoryId should wrap string correctly`() {
        val categoryId = CategoryId("CATEGORY_A")

        assertEquals("CATEGORY_A", categoryId.value)
    }

    @Test
    fun `CategoryId should have correct equality`() {
        val categoryId1 = CategoryId("CATEGORY_A")
        val categoryId2 = CategoryId("CATEGORY_A")

        assertEquals(categoryId1, categoryId2)
        assertEquals(categoryId1.hashCode(), categoryId2.hashCode())
    }

    @Test
    fun `CategoryId should be different for different values`() {
        val categoryId1 = CategoryId("CATEGORY_A")
        val categoryId2 = CategoryId("CATEGORY_B")

        assertNotEquals(categoryId1, categoryId2)
    }

    @Test
    fun `CategoryId toString should contain value`() {
        val categoryId = CategoryId("CATEGORY_A")

        assertTrue(categoryId.toString().contains("CATEGORY_A"))
    }

    @Test
    fun `CategoryId copy should work correctly`() {
        val categoryId1 = CategoryId("CATEGORY_A")
        val categoryId2 = categoryId1.copy(value = "CATEGORY_B")

        assertEquals("CATEGORY_B", categoryId2.value)
        assertNotEquals(categoryId1, categoryId2)
    }

    @Test
    fun `CategoryId should reject blank`() {
        assertThrows<IllegalArgumentException> { CategoryId("") }
        assertThrows<IllegalArgumentException> { CategoryId("   ") }
    }

    @Test
    fun `UserId copy should work correctly`() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val userId1 = UserId(uuid1)
        val userId2 = userId1.copy(value = uuid2)

        assertEquals(uuid2, userId2.value)
        assertNotEquals(userId1, userId2)
    }
}
