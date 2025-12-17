package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LegacyDataMigratorTest {

    private lateinit var categoryResolutionService: CategoryResolutionService
    private lateinit var migrator: DefaultLegacyDataMigrator

    private val categoryA = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
            NotificationType("type2", CategoryId("CATEGORY_A"), Instant.now()),
        ),
    )

    private val categoryB = NotificationCategory(
        id = CategoryId("CATEGORY_B"),
        name = "Category B",
        types = setOf(
            NotificationType("type4", CategoryId("CATEGORY_B"), Instant.now()),
            NotificationType("type5", CategoryId("CATEGORY_B"), Instant.now()),
        ),
    )

    @BeforeEach
    fun setup() {
        categoryResolutionService = mockk()
        migrator = DefaultLegacyDataMigrator(categoryResolutionService)
    }

    @Test
    fun `should migrate single type to category subscription`() {
        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1")) } returns setOf(categoryA)

        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), "type1")

        assertEquals(1, result.size)
        assertEquals("CATEGORY_A", result.first().category.id.value)
    }

    @Test
    fun `should migrate multiple types to category subscriptions`() {
        every {
            categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1", "type5"))
        } returns setOf(categoryA, categoryB)

        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), "type1;type5")

        assertEquals(2, result.size)
    }

    @Test
    fun `should handle empty legacy types`() {
        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), "")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle blank legacy types`() {
        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), "   ")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle trailing semicolons`() {
        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1")) } returns setOf(categoryA)

        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), ";type1;")

        assertEquals(1, result.size)
    }

    @Test
    fun `should trim whitespace from types`() {
        every {
            categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1", "type2"))
        } returns setOf(categoryA)

        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), " type1 ; type2 ")

        assertEquals(1, result.size)
    }

    @Test
    fun `should deduplicate types`() {
        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1")) } returns setOf(categoryA)

        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), "type1;type1;type1")

        assertEquals(1, result.size)
    }

    @Test
    fun `should handle no resolved categories`() {
        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) } returns emptySet()

        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), "unknown")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should normalize to lowercase`() {
        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1")) } returns setOf(categoryA)

        val result = migrator.migrateUserTypes(UserId(UUID.randomUUID()), "TYPE1")

        assertEquals(1, result.size)
    }
}
