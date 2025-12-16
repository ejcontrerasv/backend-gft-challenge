package de.dkb.api.codeChallenge.application.dto

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class ApplicationDtosTest {

    @Test
    fun `RegisterUserCommand should create from UUID and types`() {
        val uuid = UUID.randomUUID()
        val types = setOf("type1", "type2")

        val command = RegisterUserCommand.from(uuid, types)

        assertEquals(uuid, command.userId.value)
        assertEquals(types, command.notificationTypes)
    }

    @Test
    fun `SendNotificationCommand should create from parameters`() {
        val uuid = UUID.randomUUID()

        val command = SendNotificationCommand.from(uuid, "type1", "Test message")

        assertEquals(uuid, command.userId.value)
        assertEquals("type1", command.notificationType)
        assertEquals("Test message", command.message)
    }

    @Test
    fun `MigrateUserCommand should create from parameters`() {
        val uuid = UUID.randomUUID()

        val command = MigrateUserCommand.from(uuid, "type1;type2")

        assertEquals(uuid, command.userId.value)
        assertEquals("type1;type2", command.legacyNotificationTypes)
    }

    @Test
    fun `UserRegistrationResult Success should contain user and categories`() {
        val userId = UserId(UUID.randomUUID())
        val categories = setOf("CATEGORY_A", "CATEGORY_B")

        val result = UserRegistrationResult.Success(userId, categories)

        assertEquals(userId, result.userId)
        assertEquals(categories, result.subscribedCategories)
    }

    @Test
    fun `UserRegistrationResult Failure should contain user and errors`() {
        val userId = UserId(UUID.randomUUID())
        val errors = listOf("Error 1", "Error 2")

        val result = UserRegistrationResult.Failure(userId, errors)

        assertEquals(userId, result.userId)
        assertEquals(errors, result.errors)
    }

    @Test
    fun `NotificationResult Sent should contain message`() {
        val result = NotificationResult.Sent("Success")

        assertEquals("Success", result.message)
    }

    @Test
    fun `NotificationResult NotSent should contain reason`() {
        val result = NotificationResult.NotSent("User not subscribed")

        assertEquals("User not subscribed", result.reason)
    }

    @Test
    fun `NotificationResult UserNotFound should contain message`() {
        val result = NotificationResult.UserNotFound("User not found")

        assertEquals("User not found", result.message)
    }
}
