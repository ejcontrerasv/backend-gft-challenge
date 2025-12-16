package de.dkb.api.codeChallenge.infrastructure.gateway

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import org.junit.jupiter.api.Test
import java.util.UUID

class ConsoleNotificationGatewayTest {

    private val gateway = ConsoleNotificationGateway()

    @Test
    fun `should send notification successfully`() {
        val userId = UserId(UUID.randomUUID())

        gateway.sendNotification(userId, "type1", "Test message")

        // Console gateway just logs, no exception means success
    }

    @Test
    fun `should handle empty message`() {
        val userId = UserId(UUID.randomUUID())

        gateway.sendNotification(userId, "type1", "")
    }

    @Test
    fun `should handle special characters in message`() {
        val userId = UserId(UUID.randomUUID())

        gateway.sendNotification(userId, "type1", "Hello! @#\$%^&*()")
    }
}
