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
data class NotificationTypeEntity(
    @Id
    @Column(name = "code", length = 50)
    val code: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: NotificationCategoryEntity,

    @Column(name = "added_at", nullable = false)
    val addedAt: Instant = Instant.now(),

    @Column(name = "active", nullable = false)
    val active: Boolean = true
) {
    constructor() : this("", NotificationCategoryEntity(), Instant.now(), true)
}
