package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.application.service.DefaultSubscriptionValidator
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SubscriptionValidatorTest {

    private val validator = DefaultSubscriptionValidator()

    private val categoryA = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
        ),
    )

    private val categoryB = NotificationCategory(
        id = CategoryId("CATEGORY_B"),
        name = "Category B",
        types = setOf(
            NotificationType("type4", CategoryId("CATEGORY_B"), Instant.now()),
        ),
    )

    @Test
    fun `should validate successfully with valid categories`() {
        val result = validator.validate(UserId(UUID.randomUUID()), setOf(categoryA, categoryB))

        assertTrue(result.isValid())
    }

    @Test
    fun `should fail validation with empty categories`() {
        val result = validator.validate(UserId(UUID.randomUUID()), emptySet())

        assertFalse(result.isValid())
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.errors.any { it.contains("at least one category") })
    }

    @Test
    fun `should validate with single category`() {
        val result = validator.validate(UserId(UUID.randomUUID()), setOf(categoryA))

        assertTrue(result.isValid())
    }
}
