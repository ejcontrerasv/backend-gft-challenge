package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository

import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.UserSubscriptionEntity
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.UserSubscriptionId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for UserSubscriptionEntity
 */
@Repository
interface UserSubscriptionJpaRepository : JpaRepository<UserSubscriptionEntity, UserSubscriptionId> {

    /**
     * Find all subscriptions for a user
     */
    fun findByUserId(userId: UUID): List<UserSubscriptionEntity>

    /**
     * Find all users subscribed to a category
     */
    fun findByCategoryId(categoryId: String): List<UserSubscriptionEntity>

    /**
     * Find all active subscriptions for a user
     */
    fun findByUserIdAndActiveTrue(userId: UUID): List<UserSubscriptionEntity>

    /**
     * Check if user is subscribed to a category
     */
    fun existsByUserIdAndCategoryId(userId: UUID, categoryId: String): Boolean

    /**
     * Delete all subscriptions for a user
     */
    @Modifying
    @Query("DELETE FROM UserSubscriptionEntity s WHERE s.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: UUID)

    /**
     * Count users subscribed to a category
     */
    fun countByCategoryId(categoryId: String): Long
}
