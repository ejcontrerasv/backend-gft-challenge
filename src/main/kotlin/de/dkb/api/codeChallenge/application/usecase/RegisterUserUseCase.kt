package de.dkb.api.codeChallenge.application.usecase

import de.dkb.api.codeChallenge.application.dto.RegisterUserCommand
import de.dkb.api.codeChallenge.application.dto.UserRegistrationResult
import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.SubscriptionValidator
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Use case for registering a user with notification subscriptions.
 * Accepts legacy type-based format and converts to category-based model.
 */
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val categoryResolutionService: CategoryResolutionService,
    private val subscriptionValidator: SubscriptionValidator
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: RegisterUserCommand): UserRegistrationResult {
        logger.info("Registering user ${command.userId.value} with types: ${command.notificationTypes}")

        try {
            // Resolve categories from incoming types (backward compatible)
            val categories = categoryResolutionService
                .resolveCategoriesFromLegacyTypes(command.notificationTypes)

            if (categories.isEmpty()) {
                logger.warn("No valid categories found for user ${command.userId.value}")
                return UserRegistrationResult.Failure(
                    userId = command.userId,
                    errors = listOf("No valid notification types provided")
                )
            }

            // Validate business rules
            val validationResult = subscriptionValidator.validate(command.userId, categories)
            if (!validationResult.isValid()) {
                logger.warn("Validation failed for user ${command.userId.value}: ${validationResult.getErrors()}")
                return UserRegistrationResult.Failure(
                    userId = command.userId,
                    errors = validationResult.getErrors()
                )
            }

            // Create user with category subscriptions
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

            // Save user
            userRepository.save(user)

            logger.info("Successfully registered user ${command.userId.value} to ${categories.size} categories")

            return UserRegistrationResult.Success(
                userId = command.userId,
                subscribedCategories = categories.map { it.id.value }.toSet()
            )

        } catch (e: Exception) {
            logger.error("Failed to register user ${command.userId.value}", e)
            return UserRegistrationResult.Failure(
                userId = command.userId,
                errors = listOf("Registration failed: ${e.message}")
            )
        }
    }
}
