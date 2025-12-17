package de.dkb.api.codeChallenge.infrastructure.config

import de.dkb.api.codeChallenge.application.usecase.MigrateUserSubscriptionsUseCase
import de.dkb.api.codeChallenge.application.usecase.RegisterUserUseCase
import de.dkb.api.codeChallenge.application.usecase.SendNotificationUseCase
import de.dkb.api.codeChallenge.domain.repository.CategoryConfigRepository
import de.dkb.api.codeChallenge.domain.repository.NotificationGateway
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.DefaultCategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.DefaultLegacyDataMigrator
import de.dkb.api.codeChallenge.domain.service.DefaultSubscriptionValidator
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
import de.dkb.api.codeChallenge.domain.service.SubscriptionValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration for use cases and domain services.
 * Wires up the hexagonal architecture components.
 */
@Configuration
class UseCaseConfiguration {

    /**
     * Domain Services
     */
    @Bean
    fun categoryResolutionService(
        categoryConfigRepository: CategoryConfigRepository
    ): CategoryResolutionService {
        return DefaultCategoryResolutionService(categoryConfigRepository)
    }

    @Bean
    fun subscriptionValidator(): SubscriptionValidator {
        return DefaultSubscriptionValidator()
    }

    @Bean
    fun legacyDataMigrator(
        categoryResolutionService: CategoryResolutionService
    ): LegacyDataMigrator {
        return DefaultLegacyDataMigrator(categoryResolutionService)
    }

    /**
     * Use Cases (Application Services)
     */
    @Bean
    fun registerUserUseCase(
        userRepository: UserRepository,
        categoryResolutionService: CategoryResolutionService,
        subscriptionValidator: SubscriptionValidator
    ): RegisterUserUseCase {
        return RegisterUserUseCase(
            userRepository = userRepository,
            categoryResolutionService = categoryResolutionService,
            subscriptionValidator = subscriptionValidator
        )
    }

    @Bean
    fun sendNotificationUseCase(
        userRepository: UserRepository,
        categoryResolutionService: CategoryResolutionService,
        notificationGateway: NotificationGateway
    ): SendNotificationUseCase {
        return SendNotificationUseCase(
            userRepository = userRepository,
            categoryResolutionService = categoryResolutionService,
            notificationGateway = notificationGateway
        )
    }

    @Bean
    fun migrateUserSubscriptionsUseCase(
        userRepository: UserRepository,
        legacyDataMigrator: LegacyDataMigrator
    ): MigrateUserSubscriptionsUseCase {
        return MigrateUserSubscriptionsUseCase(
            userRepository = userRepository,
            legacyDataMigrator = legacyDataMigrator
        )
    }
}
