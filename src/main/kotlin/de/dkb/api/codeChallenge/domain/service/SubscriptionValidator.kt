package de.dkb.api.codeChallenge.domain.service

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId

/**
 * Domain service for validating subscription business rules
 */
interface SubscriptionValidator {
    /**
     * Validate subscription request
     */
    fun validate(userId: UserId, categories: Set<NotificationCategory>): ValidationResult
}

/**
 * Result of validation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()

    fun isValid(): Boolean = this is Valid
}

