package de.dkb.api.codeChallenge.application.service

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.service.SubscriptionValidator
import de.dkb.api.codeChallenge.domain.service.ValidationResult

class DefaultSubscriptionValidator : SubscriptionValidator {

    override fun validate(userId: UserId, categories: Set<NotificationCategory>): ValidationResult {
        val errors = mutableListOf<String>()

        if (categories.isEmpty()) {
            errors.add("User must subscribe to at least one category")
        }

        categories.forEach { category ->
            if (category.id.value.isBlank()) {
                errors.add("Category ID cannot be blank")
            }
        }

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

