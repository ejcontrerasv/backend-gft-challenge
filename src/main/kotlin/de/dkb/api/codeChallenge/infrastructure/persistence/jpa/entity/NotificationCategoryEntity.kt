package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * JPA entity for notification_categories table
 */
@Entity
@Table(name = "notification_categories")
class NotificationCategoryEntity(
    @Id
    @Column(name = "id", length = 50)
    var id: String = "",

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @OneToMany(mappedBy = "category", fetch = FetchType.EAGER)
    var types: MutableSet<NotificationTypeEntity> = mutableSetOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotificationCategoryEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "NotificationCategoryEntity(id='$id', name='$name')"
}
