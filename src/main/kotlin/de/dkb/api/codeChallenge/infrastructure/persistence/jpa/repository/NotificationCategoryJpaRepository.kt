package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository

import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.NotificationCategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for NotificationCategoryEntity
 */
@Repository
interface NotificationCategoryJpaRepository : JpaRepository<NotificationCategoryEntity, String> {

    /**
     * Find all categories with their types eagerly loaded
     */
    @Query("SELECT DISTINCT c FROM NotificationCategoryEntity c LEFT JOIN FETCH c.types")
    fun findAllWithTypes(): List<NotificationCategoryEntity>
}
