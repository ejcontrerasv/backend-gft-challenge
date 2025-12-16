package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.time.Instant

/**
 * JPA entity for notification_categories table
 */
@Entity
@Table(name = "notification_categories")
data class NotificationCategoryEntity(
    @Id
    @Column(name = "id", length = 50)
    val id: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "category", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val types: Set<NotificationTypeEntity> = emptySet()
) {
    constructor() : this("", "", Instant.now(), Instant.now(), emptySet())
}
