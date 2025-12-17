package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity

import jakarta.persistence.*
import java.util.UUID

/**
 * JPA entity for users_legacy table (formerly 'users').
 * Used during dual-write migration period.
 * Will be removed once migration is complete.
 */
@Entity
@Table(name = "users_legacy")
data class LegacyUserEntity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID,

    @Convert(converter = LegacyNotificationTypeSetConverter::class)
    @Column(name = "notifications", nullable = false)
    var notifications: MutableSet<LegacyNotificationType> = mutableSetOf(),
) {
    constructor() : this(UUID.randomUUID(), mutableSetOf())

    /**
     * Get notifications as semicolon-separated string (for migration)
     */
    fun getNotificationsAsString(): String = notifications.joinToString(";") { it.name }
}
