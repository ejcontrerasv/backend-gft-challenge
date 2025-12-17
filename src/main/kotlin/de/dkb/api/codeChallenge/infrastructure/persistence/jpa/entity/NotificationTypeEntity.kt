package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * JPA entity for notification_types table
 */
@Entity
@Table(
    name = "notification_types",
    indexes = [Index(name = "idx_notification_types_category", columnList = "category_id")]
)
class NotificationTypeEntity(
    @Id
    @Column(name = "code", length = 50)
    var code: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: NotificationCategoryEntity? = null,

    @Column(name = "added_at", nullable = false)
    var addedAt: Instant = Instant.now(),

    @Column(name = "active", nullable = false)
    var active: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationTypeEntity) return false
        return code == other.code
    }

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = "NotificationTypeEntity(code='$code', active=$active)"
}
