package de.dkb.api.codeChallenge.domain.model.valueobject

import java.util.UUID

/**
 * Value object representing a unique user identifier.
 * Encapsulates UUID to provide type safety and domain semantics.
 */
data class UserId(val value: UUID) {
    companion object {
        fun fromString(id: String): UserId = UserId(UUID.fromString(id))
        fun random(): UserId = UserId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
