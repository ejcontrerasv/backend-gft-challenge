package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource

/**
 * Strategy interface to perform read operations from repositories depending on a migration read source.
 */
interface UserReadStrategy {
    val type: ReadSource

    fun findById(id: UserId): User?
    fun existsById(id: UserId): Boolean
    fun count(): Long
    fun findAll(): List<User>
}
