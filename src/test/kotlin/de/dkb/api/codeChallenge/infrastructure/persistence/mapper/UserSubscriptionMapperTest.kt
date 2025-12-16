package de.dkb.api.codeChallenge.infrastructure.persistence.mapper

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.UserSubscriptionEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UserSubscriptionMapperTest {

    private val categoryA = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
        ),
    )

    @Test
    fun `should map entities to domain user`() {
        val userId = UUID.randomUUID()
        val entities = listOf(
            UserSubscriptionEntity(
                userId = userId,
                categoryId = "CATEGORY_A",
                subscribedAt = Instant.now(),
                active = true,
            ),
        )
        val categoryMap = mapOf("CATEGORY_A" to categoryA)

        val user = UserSubscriptionMapper.toDomain(userId, entities, categoryMap)

        assertEquals(userId, user.id.value)
        assertEquals(1, user.subscriptions.size)
    }

    @Test
    fun `should map user to entities`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(
            id = userId,
            subscriptions = setOf(
                CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            ),
        )

        val entities = UserSubscriptionMapper.toEntities(user)

        assertEquals(1, entities.size)
        assertEquals(userId.value, entities.first().userId)
        assertEquals("CATEGORY_A", entities.first().categoryId)
    }

    @Test
    fun `should handle empty subscriptions`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(id = userId, subscriptions = emptySet())

        val entities = UserSubscriptionMapper.toEntities(user)

        assertTrue(entities.isEmpty())
    }

    @Test
    fun `should skip unknown categories in domain mapping`() {
        val userId = UUID.randomUUID()
        val entities = listOf(
            UserSubscriptionEntity(
                userId = userId,
                categoryId = "UNKNOWN_CATEGORY",
                subscribedAt = Instant.now(),
                active = true,
            ),
        )
        val categoryMap = mapOf("CATEGORY_A" to categoryA)

        val user = UserSubscriptionMapper.toDomain(userId, entities, categoryMap)

        assertEquals(0, user.subscriptions.size)
    }
}
