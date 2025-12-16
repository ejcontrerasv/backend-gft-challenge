package de.dkb.api.codeChallenge.application.dto

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId

/**
 * Result of user registration operation
 */
sealed class UserRegistrationResult {
    data class Success(
        val userId: UserId,
        val subscribedCategories: Set<String>
    ) : UserRegistrationResult()

    data class Failure(
        val userId: UserId,
        val errors: List<String>
    ) : UserRegistrationResult()
}
