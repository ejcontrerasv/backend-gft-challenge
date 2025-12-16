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
    fun getErrors(): List<String> = when (this) {
        is Invalid -> errors
        is Valid -> emptyList()
    }
}

/**
 * Default implementation of SubscriptionValidator
 */
class DefaultSubscriptionValidator : SubscriptionValidator {

    override fun validate(userId: UserId, categories: Set<NotificationCategory>): ValidationResult {
        val errors = mutableListOf<String>()

        // Business rule: User must subscribe to at least one category
        if (categories.isEmpty()) {
            errors.add("User must subscribe to at least one category")
        }

        // Business rule: Categories must have valid IDs
        categories.forEach { category ->
            if (category.id.value.isBlank()) {
                errors.add("Category ID cannot be blank")
            }
        }

        // Business rule: Categories must have at least one type
        categories.forEach { category ->
            if (category.types.isEmpty()) {
                errors.add("Category ${category.id.value} has no notification types")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
