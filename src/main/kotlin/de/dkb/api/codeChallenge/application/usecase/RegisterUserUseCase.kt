package de.dkb.api.codeChallenge.application.usecase

import de.dkb.api.codeChallenge.application.dto.RegisterUserCommand
import de.dkb.api.codeChallenge.application.dto.UserRegistrationResult
import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.SubscriptionValidator
import de.dkb.api.codeChallenge.domain.service.ValidationResult
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Use case for registering a user with notification subscriptions.
 * Accepts legacy type-based format and converts to category-based model.
 */
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val categoryResolutionService: CategoryResolutionService,
    private val subscriptionValidator: SubscriptionValidator
) {

    fun execute(command: RegisterUserCommand): UserRegistrationResult {
        logger.info { "Registering user ${command.userId.value} with types: ${command.notificationTypes}" }

        try {
            val categories = categoryResolutionService
                .resolveCategoriesFromLegacyTypes(command.notificationTypes)

            if (categories.isEmpty()) {
                logger.warn { "No valid categories found for user ${command.userId.value}" }
                return UserRegistrationResult.Failure(
                    userId = command.userId,
                    errors = listOf("No valid notification types provided")
                )
            }

            val validationResult = subscriptionValidator.validate(command.userId, categories)
            if (!validationResult.isValid()) {
                val errors = (validationResult as ValidationResult.Invalid).errors
                logger.warn { "Validation failed for user ${command.userId.value}: $errors" }
                return UserRegistrationResult.Failure(
                    userId = command.userId,
                    errors = errors
                )
            }

            val subscriptions = categories.map { category ->
                CategorySubscription(
                    category = category,
                    subscribedAt = Instant.now(),
                    active = true
                )
            }.toSet()

            val user = User(
                id = command.userId,
                subscriptions = subscriptions
            )

            userRepository.save(user)

            logger.info { "Successfully registered user ${command.userId.value} to ${categories.size} categories" }

            return UserRegistrationResult.Success(
                userId = command.userId,
                subscribedCategories = categories.map { it.id.value }.toSet()
            )

        } catch (e: Exception) {
            logger.error(e) { "Failed to register user ${command.userId.value}" }
            return UserRegistrationResult.Failure(
                userId = command.userId,
                errors = listOf("Registration failed: ${e.message}")
            )
        }
    }
}
