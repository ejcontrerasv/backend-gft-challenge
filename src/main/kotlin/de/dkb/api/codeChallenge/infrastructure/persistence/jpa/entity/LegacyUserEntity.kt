package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import de.dkb.api.codeChallenge.notification.model.NotificationType
import de.dkb.api.codeChallenge.notification.model.NotificationTypeSetConverter
import jakarta.persistence.*
import java.util.UUID

/**
 * JPA entity for users_legacy table (formerly 'users').
 * Used during dual-write migration period.
 */
@Entity
@Table(name = "users_legacy")
data class LegacyUserEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,

    @Convert(converter = NotificationTypeSetConverter::class)
    @Column(name = "notifications", nullable = false)
    var notifications: MutableSet<NotificationType> = mutableSetOf()
) {
    constructor() : this(UUID.randomUUID(), mutableSetOf())

    /**
     * Get notifications as semicolon-separated string (for migration)
     */
    fun getNotificationsAsString(): String {
        return notifications.joinToString(";") { it.name }
    }
}
