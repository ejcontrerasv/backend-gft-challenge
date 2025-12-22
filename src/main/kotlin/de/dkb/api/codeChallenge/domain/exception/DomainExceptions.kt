package de.dkb.api.codeChallenge.domain.exception

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId

/**
 * Base exception for domain-level errors.
 * All domain exceptions should extend this class.
 */
sealed class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when a user is not found in the system.
 */
class UserNotFoundException(userId: UserId) : DomainException("User ${userId.value} not found")

/**
 * Thrown when a user already exists in the system.
 */
class UserAlreadyExistsException(userId: UserId) : DomainException("User ${userId.value} already exists")

/**
 * Thrown when an invalid notification type is provided.
 */
class InvalidNotificationTypeException(typeCode: String) : DomainException("Invalid notification type: $typeCode")

/**
 * Thrown when an invalid category is referenced.
 */
class InvalidCategoryException(categoryId: String) : DomainException("Invalid category: $categoryId")

/**
 * Thrown when subscription validation fails.
 */
class SubscriptionValidationException(errors: List<String>) : DomainException("Subscription validation failed: ${errors.joinToString(", ")}")

/**
 * Thrown when migration of legacy data fails.
 */
class MigrationException(message: String, cause: Throwable? = null) : DomainException(message, cause)
