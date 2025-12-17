package de.dkb.api.codeChallenge.domain.model

import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserTest {

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

    @Test
    fun `should create user with subscriptions`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
        )

        val user = User(id = userId, subscriptions = subscriptions)

        assertEquals(userId, user.id)
        assertEquals(1, user.subscriptions.size)
    }

    @Test
    fun `should check if user can receive notification type`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
        )
        val user = User(id = userId, subscriptions = subscriptions)

        assertTrue(user.canReceiveNotificationType("type1"))
        assertTrue(user.canReceiveNotificationType("type2"))
        assertTrue(user.canReceiveNotificationType("type3"))
        assertFalse(user.canReceiveNotificationType("type4"))
        assertFalse(user.canReceiveNotificationType("type5"))
    }

    @Test
    fun `should return false for unknown notification type`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
        )
        val user = User(id = userId, subscriptions = subscriptions)

        assertFalse(user.canReceiveNotificationType("unknown_type"))
    }

    @Test
    fun `should get all subscribed type codes`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            CategorySubscription(category = categoryB, subscribedAt = Instant.now(), active = true),
        )
        val user = User(id = userId, subscriptions = subscriptions)

        val typeCodes = user.getAllSubscribedTypeCodes()

        assertEquals(5, typeCodes.size)
        assertTrue(typeCodes.containsAll(setOf("type1", "type2", "type3", "type4", "type5")))
    }

    @Test
    fun `should get active category ids`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            CategorySubscription(category = categoryB, subscribedAt = Instant.now(), active = true),
        )
        val user = User(id = userId, subscriptions = subscriptions)

        val categoryIds = user.getActiveCategoryIds()

        assertEquals(2, categoryIds.size)
        assertTrue(categoryIds.contains(CategoryId("CATEGORY_A")))
        assertTrue(categoryIds.contains(CategoryId("CATEGORY_B")))
    }

    @Test
    fun `should check if user is subscribed to category`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
        )
        val user = User(id = userId, subscriptions = subscriptions)

        assertTrue(user.isSubscribedToCategory(CategoryId("CATEGORY_A")))
        assertFalse(user.isSubscribedToCategory(CategoryId("CATEGORY_B")))
    }

    @Test
    fun `should handle user with no subscriptions`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(id = userId, subscriptions = emptySet())

        assertFalse(user.canReceiveNotificationType("type1"))
        assertTrue(user.getAllSubscribedTypeCodes().isEmpty())
        assertTrue(user.getActiveCategoryIds().isEmpty())
    }

    @Test
    fun `should add subscription to user`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(id = userId, subscriptions = emptySet())
        val subscription = CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true)

        val updatedUser = user.addSubscription(subscription)

        assertEquals(1, updatedUser.subscriptions.size)
        assertTrue(updatedUser.isSubscribedToCategory("CATEGORY_A"))
    }

    @Test
    fun `should add multiple subscriptions to user`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(id = userId, subscriptions = emptySet())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            CategorySubscription(category = categoryB, subscribedAt = Instant.now(), active = true),
        )

        val updatedUser = user.addSubscriptions(subscriptions)

        assertEquals(2, updatedUser.subscriptions.size)
        assertTrue(updatedUser.isSubscribedToCategory("CATEGORY_A"))
        assertTrue(updatedUser.isSubscribedToCategory("CATEGORY_B"))
    }

    @Test
    fun `should check subscription by string category id`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
        )
        val user = User(id = userId, subscriptions = subscriptions)

        assertTrue(user.isSubscribedToCategory("CATEGORY_A"))
        assertFalse(user.isSubscribedToCategory("CATEGORY_B"))
    }

    @Test
    fun `should not include inactive subscriptions in type codes`() {
        val userId = UserId(UUID.randomUUID())
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = false),
        )
        val user = User(id = userId, subscriptions = subscriptions)

        assertTrue(user.getAllSubscribedTypeCodes().isEmpty())
        assertFalse(user.canReceiveNotificationType("type1"))
    }
}
