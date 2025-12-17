package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for user_subscriptions table
 */
@Entity
@Table(
    name = "user_subscriptions",
    indexes = [
        Index(name = "idx_user_subscriptions_category", columnList = "category_id"),
        Index(name = "idx_user_subscriptions_user", columnList = "user_id"),
    ],
)
@IdClass(UserSubscriptionId::class)
data class UserSubscriptionEntity(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Id
    @Column(name = "category_id", length = 50, nullable = false)
    val categoryId: String,

    @Column(name = "subscribed_at", nullable = false)
    val subscribedAt: Instant = Instant.now(),

    @Column(name = "active", nullable = false)
    val active: Boolean = true,
) {
    constructor() : this(UUID.randomUUID(), "", Instant.now(), true)
}

/**
 * Composite primary key for UserSubscriptionEntity
 */
@Embeddable
data class UserSubscriptionId(val userId: UUID = UUID.randomUUID(), val categoryId: String = "") : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserSubscriptionId) return false
        return userId == other.userId && categoryId == other.categoryId
    }

    override fun hashCode(): Int = userId.hashCode() * 31 + categoryId.hashCode()
}
