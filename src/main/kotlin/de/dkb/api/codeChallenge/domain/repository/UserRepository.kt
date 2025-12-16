package de.dkb.api.codeChallenge.domain.repository

import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId

/**
 * Repository interface (output port) for User aggregate.
 * Defines operations for persisting and retrieving users.
 * Implementation will be provided by infrastructure layer.
 */
interface UserRepository {
    /**
     * Find a user by their unique identifier
     */
    fun findById(id: UserId): User?

    /**
     * Save or update a user
     */
    fun save(user: User): User

    /**
     * Find all users subscribed to a specific category
     */
    fun findByCategory(categoryId: CategoryId): List<User>

    /**
     * Check if a user exists
     */
    fun existsById(id: UserId): Boolean

    /**
     * Get total count of users
     */
    fun count(): Long

    /**
     * Find all users (for migration purposes)
     */
    fun findAll(): List<User>
}
