package de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository

import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.LegacyUserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for LegacyUserEntity (users_legacy table).
 * Used during dual-write migration period.
 */
@Repository
interface LegacyUserJpaRepository : JpaRepository<LegacyUserEntity, UUID> {

    /**
     * Find all users with pagination (for batch migration)
     */
    override fun findAll(pageable: Pageable): Page<LegacyUserEntity>

    /**
     * Check if user exists in legacy table
     */
    override fun existsById(id: UUID): Boolean
}
