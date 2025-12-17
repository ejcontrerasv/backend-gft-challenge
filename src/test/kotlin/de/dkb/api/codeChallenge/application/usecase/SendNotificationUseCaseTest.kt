package de.dkb.api.codeChallenge.application.usecase

import de.dkb.api.codeChallenge.application.dto.NotificationResult
import de.dkb.api.codeChallenge.application.dto.SendNotificationCommand
import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.repository.NotificationGateway
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SendNotificationUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var categoryResolutionService: CategoryResolutionService
    private lateinit var notificationGateway: NotificationGateway
    private lateinit var useCase: SendNotificationUseCase

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
        userRepository = mockk()
        categoryResolutionService = mockk()
        notificationGateway = mockk(relaxed = true)
        useCase = SendNotificationUseCase(userRepository, categoryResolutionService, notificationGateway)
    }

    @Test
    fun `should send notification successfully`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(
            id = userId,
            subscriptions = setOf(
                CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            ),
        )
        val command = SendNotificationCommand(userId, "type1", "Test message")

        every { userRepository.findById(userId) } returns user
        every { categoryResolutionService.isValidTypeCode("type1") } returns true

        val result = useCase.execute(command)

        assertTrue(result is NotificationResult.Sent)
        verify { notificationGateway.sendNotification(userId, "type1", "Test message") }
    }

    @Test
    fun `should return UserNotFound when user does not exist`() {
        val userId = UserId(UUID.randomUUID())
        val command = SendNotificationCommand(userId, "type1", "Test message")

        every { userRepository.findById(userId) } returns null

        val result = useCase.execute(command)

        assertTrue(result is NotificationResult.UserNotFound)
    }

    @Test
    fun `should return NotSent for invalid notification type`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(id = userId, subscriptions = emptySet())
        val command = SendNotificationCommand(userId, "invalid_type", "Test message")

        every { userRepository.findById(userId) } returns user
        every { categoryResolutionService.isValidTypeCode("invalid_type") } returns false

        val result = useCase.execute(command)

        assertTrue(result is NotificationResult.NotSent)
        val notSent = result as NotificationResult.NotSent
        assertTrue(notSent.reason.contains("Invalid notification type"))
    }

    @Test
    fun `should return NotSent when user not subscribed`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(id = userId, subscriptions = emptySet())
        val command = SendNotificationCommand(userId, "type1", "Test message")

        every { userRepository.findById(userId) } returns user
        every { categoryResolutionService.isValidTypeCode("type1") } returns true

        val result = useCase.execute(command)

        assertTrue(result is NotificationResult.NotSent)
        val notSent = result as NotificationResult.NotSent
        assertTrue(notSent.reason.contains("not subscribed"))
    }

    @Test
    fun `should handle gateway exception`() {
        val userId = UserId(UUID.randomUUID())
        val user = User(
            id = userId,
            subscriptions = setOf(
                CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            ),
        )
        val command = SendNotificationCommand(userId, "type1", "Test message")

        every { userRepository.findById(userId) } returns user
        every { categoryResolutionService.isValidTypeCode("type1") } returns true
        every { notificationGateway.sendNotification(any(), any(), any()) } throws RuntimeException("Gateway error")

        val result = useCase.execute(command)

        assertTrue(result is NotificationResult.NotSent)
        val notSent = result as NotificationResult.NotSent
        assertTrue(notSent.reason.contains("Gateway error"))
    }

    @Test
    fun `should create command from factory method`() {
        val uuid = UUID.randomUUID()
        val command = SendNotificationCommand.from(uuid, "type1", "Test message")

        assertEquals(uuid, command.userId.value)
        assertEquals("type1", command.notificationType)
        assertEquals("Test message", command.message)
    }
}
