package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId

/**
 * Domain service for migrating legacy type-based data to category-based model.
 */
interface LegacyDataMigrator {
    /**
     * Migrate legacy notification types string to category subscriptions
     *
     * Examples:
     * - "type1" -> [CategoryA]
     * - "type1;type2;type3" -> [CategoryA]
     * - "type1;type5" -> [CategoryA, CategoryB]
     * - "type1;type2;type4;type5" -> [CategoryA, CategoryB]
     */
    fun migrateUserTypes(userId: UserId, legacyTypes: String): Set<CategorySubscription>
}
