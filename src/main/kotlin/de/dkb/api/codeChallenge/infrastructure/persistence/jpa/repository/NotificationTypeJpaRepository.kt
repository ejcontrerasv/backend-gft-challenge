package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository

import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.NotificationTypeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for NotificationTypeEntity
 */
@Repository
interface NotificationTypeJpaRepository : JpaRepository<NotificationTypeEntity, String> {

    /**
     * Find all types belonging to a specific category
     */
    @Query("SELECT t FROM NotificationTypeEntity t WHERE t.category.id = :categoryId")
    fun findByCategoryId(@Param("categoryId") categoryId: String): List<NotificationTypeEntity>

    /**
     * Find all active types
     */
    fun findByActiveTrue(): List<NotificationTypeEntity>

    /**
     * Find category by type code
     */
    @Query("SELECT t.category FROM NotificationTypeEntity t WHERE t.code = :typeCode")
    fun findCategoryByTypeCode(@Param("typeCode") typeCode: String): de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.NotificationCategoryEntity?
}
