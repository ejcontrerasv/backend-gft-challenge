package de.dkb.api.codeChallenge.infrastructure.persistence.mapper

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.UserSubscriptionEntity
import java.util.UUID

/**
 * Mapper for converting between User/CategorySubscription domain models and JPA entities
 */
object UserSubscriptionMapper {

    fun toDomain(
        userId: UUID,
        subscriptionEntities: List<UserSubscriptionEntity>,
        categoryMap: Map<String, NotificationCategory>,
    ): User {
        val subscriptions = subscriptionEntities.mapNotNull { entity ->
            val category = categoryMap[entity.categoryId]
            if (category != null) {
                CategorySubscription(
                    category = category,
                    subscribedAt = entity.subscribedAt,
                    active = entity.active,
                )
            } else {
                null
            }
        }.toSet()

        return User(
            id = UserId(userId),
            subscriptions = subscriptions,
        )
    }

    fun toEntities(user: User): List<UserSubscriptionEntity> = user.subscriptions.map { subscription ->
        UserSubscriptionEntity(
            userId = user.id.value,
            categoryId = subscription.category.id.value,
            subscribedAt = subscription.subscribedAt,
            active = subscription.active,
        )
    }
}
