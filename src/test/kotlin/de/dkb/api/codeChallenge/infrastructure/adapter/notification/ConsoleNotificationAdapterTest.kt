package de.dkb.api.codeChallenge.infrastructure.adapter.notification

import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import org.junit.jupiter.api.Test
import java.util.UUID

class ConsoleNotificationAdapterTest {

    private val adapter = ConsoleNotificationAdapter()

    @Test
    fun `should send notification successfully`() {
        val userId = UserId(UUID.randomUUID())

        adapter.sendNotification(userId, "type1", "Test message")

        // Console adapter just logs, no exception means success
    }

    @Test
    fun `should handle empty message`() {
        val userId = UserId(UUID.randomUUID())

        adapter.sendNotification(userId, "type1", "")
    }

    @Test
    fun `should handle special characters in message`() {
        val userId = UserId(UUID.randomUUID())

        adapter.sendNotification(userId, "type1", "Hello! @#\$%^&*()")
    }

    @Test
    fun `should send bulk notifications`() {
        val userIds = listOf(
            UserId(UUID.randomUUID()),
            UserId(UUID.randomUUID()),
            UserId(UUID.randomUUID()),
        )

        adapter.sendBulkNotification(userIds, "type1", "Bulk message")
    }
}
