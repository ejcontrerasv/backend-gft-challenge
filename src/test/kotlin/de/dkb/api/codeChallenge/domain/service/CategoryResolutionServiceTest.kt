package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.repository.CategoryConfigRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class CategoryResolutionServiceTest {

    private lateinit var categoryConfigRepository: CategoryConfigRepository
    private lateinit var service: DefaultCategoryResolutionService

    private val categoryA = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
            NotificationType("type2", CategoryId("CATEGORY_A"), Instant.now()),
            NotificationType("type3", CategoryId("CATEGORY_A"), Instant.now()),
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
        categoryConfigRepository = mockk()
        service = DefaultCategoryResolutionService(categoryConfigRepository)
    }

    @Test
    fun `should resolve category by type code`() {
        every { categoryConfigRepository.findCategoryByTypeCode("type1") } returns categoryA

        val result = service.resolveCategoryByTypeCode("type1")

        assertNotNull(result)
        assertEquals("CATEGORY_A", result?.id?.value)
    }

    @Test
    fun `should return null for unknown type code`() {
        every { categoryConfigRepository.findCategoryByTypeCode("unknown") } returns null

        val result = service.resolveCategoryByTypeCode("unknown")

        assertNull(result)
    }

    @Test
    fun `should normalize type code before resolution`() {
        every { categoryConfigRepository.findCategoryByTypeCode("type1") } returns categoryA

        val result = service.resolveCategoryByTypeCode("  TYPE1  ")

        assertNotNull(result)
        assertEquals("CATEGORY_A", result?.id?.value)
    }

    @Test
    fun `should resolve categories from legacy types`() {
        every { categoryConfigRepository.findCategoryByTypeCode("type1") } returns categoryA
        every { categoryConfigRepository.findCategoryByTypeCode("type5") } returns categoryB

        val result = service.resolveCategoriesFromLegacyTypes(setOf("type1", "type5"))

        assertEquals(2, result.size)
        assertTrue(result.any { it.id.value == "CATEGORY_A" })
        assertTrue(result.any { it.id.value == "CATEGORY_B" })
    }

    @Test
    fun `should skip unknown types in legacy resolution`() {
        every { categoryConfigRepository.findCategoryByTypeCode("type1") } returns categoryA
        every { categoryConfigRepository.findCategoryByTypeCode("unknown") } returns null

        val result = service.resolveCategoriesFromLegacyTypes(setOf("type1", "unknown"))

        assertEquals(1, result.size)
        assertEquals("CATEGORY_A", result.first().id.value)
    }

    @Test
    fun `should deduplicate categories`() {
        every { categoryConfigRepository.findCategoryByTypeCode("type1") } returns categoryA
        every { categoryConfigRepository.findCategoryByTypeCode("type2") } returns categoryA

        val result = service.resolveCategoriesFromLegacyTypes(setOf("type1", "type2"))

        assertEquals(1, result.size)
        assertEquals("CATEGORY_A", result.first().id.value)
    }

    @Test
    fun `should validate type code exists`() {
        every { categoryConfigRepository.findCategoryByTypeCode("type1") } returns categoryA
        every { categoryConfigRepository.findCategoryByTypeCode("invalid") } returns null

        assertTrue(service.isValidTypeCode("type1"))
        assertFalse(service.isValidTypeCode("invalid"))
    }
}
