package de.dkb.api.codeChallenge.domain.model

import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class CategorySubscriptionTest {

    private val category = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
        ),
    )

    @Test
    fun `should create active subscription`() {
        val now = Instant.now()
        val subscription = CategorySubscription(
            category = category,
            subscribedAt = now,
            active = true,
        )

        assertEquals(category, subscription.category)
        assertEquals(now, subscription.subscribedAt)
        assertTrue(subscription.active)
    }

    @Test
    fun `should create inactive subscription`() {
        val subscription = CategorySubscription(
            category = category,
            subscribedAt = Instant.now(),
            active = false,
        )

        assertFalse(subscription.active)
    }

    @Test
    fun `should have correct equality`() {
        val now = Instant.now()
        val subscription1 = CategorySubscription(category = category, subscribedAt = now, active = true)
        val subscription2 = CategorySubscription(category = category, subscribedAt = now, active = true)

        assertEquals(subscription1, subscription2)
    }
}
