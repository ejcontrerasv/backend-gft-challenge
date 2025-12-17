package de.dkb.api.codeChallenge.application.usecase

import de.dkb.api.codeChallenge.application.dto.RegisterUserCommand
import de.dkb.api.codeChallenge.application.dto.UserRegistrationResult
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.domain.service.SubscriptionValidator
import de.dkb.api.codeChallenge.domain.service.ValidationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RegisterUserUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var categoryResolutionService: CategoryResolutionService
    private lateinit var subscriptionValidator: SubscriptionValidator
    private lateinit var useCase: RegisterUserUseCase

    private val categoryA = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
            NotificationType("type2", CategoryId("CATEGORY_A"), Instant.now()),
        ),
    )

    @BeforeEach
    fun setup() {
        userRepository = mockk(relaxed = true)
        categoryResolutionService = mockk()
        subscriptionValidator = mockk()
        useCase = RegisterUserUseCase(userRepository, categoryResolutionService, subscriptionValidator)
    }

    @Test
    fun `should register user successfully`() {
        val userId = UserId(UUID.randomUUID())
        val command = RegisterUserCommand(userId, setOf("type1", "type2"))

        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) } returns setOf(categoryA)
        every { subscriptionValidator.validate(any(), any()) } returns ValidationResult.Valid

        val result = useCase.execute(command)

        assertTrue(result is UserRegistrationResult.Success)
        val success = result as UserRegistrationResult.Success
        assertEquals(userId, success.userId)
        assertTrue(success.subscribedCategories.contains("CATEGORY_A"))
        verify { userRepository.save(any()) }
    }

    @Test
    fun `should fail when no valid categories found`() {
        val userId = UserId(UUID.randomUUID())
        val command = RegisterUserCommand(userId, setOf("unknown"))

        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) } returns emptySet()

        val result = useCase.execute(command)

        assertTrue(result is UserRegistrationResult.Failure)
        val failure = result as UserRegistrationResult.Failure
        assertTrue(failure.errors.any { it.contains("No valid notification types") })
    }

    @Test
    fun `should fail when validation fails`() {
        val userId = UserId(UUID.randomUUID())
        val command = RegisterUserCommand(userId, setOf("type1"))

        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) } returns setOf(categoryA)
        every { subscriptionValidator.validate(any(), any()) } returns ValidationResult.Invalid(listOf("Validation error"))

        val result = useCase.execute(command)

        assertTrue(result is UserRegistrationResult.Failure)
        val failure = result as UserRegistrationResult.Failure
        assertTrue(failure.errors.contains("Validation error"))
    }

    @Test
    fun `should handle exception during save`() {
        val userId = UserId(UUID.randomUUID())
        val command = RegisterUserCommand(userId, setOf("type1"))

        every { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) } returns setOf(categoryA)
        every { subscriptionValidator.validate(any(), any()) } returns ValidationResult.Valid
        every { userRepository.save(any()) } throws RuntimeException("Database error")

        val result = useCase.execute(command)

        assertTrue(result is UserRegistrationResult.Failure)
        val failure = result as UserRegistrationResult.Failure
        assertTrue(failure.errors.any { it.contains("Database error") })
    }

    @Test
    fun `should create command from factory method`() {
        val uuid = UUID.randomUUID()
        val command = RegisterUserCommand.from(uuid, setOf("type1", "type2"))

        assertEquals(uuid, command.userId.value)
        assertEquals(setOf("type1", "type2"), command.notificationTypes)
    }
}
