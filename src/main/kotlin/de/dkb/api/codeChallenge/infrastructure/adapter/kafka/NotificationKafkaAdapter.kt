package de.dkb.api.codeChallenge.infrastructure.adapter.kafka

import de.dkb.api.codeChallenge.application.dto.SendNotificationCommand
import de.dkb.api.codeChallenge.application.usecase.SendNotificationUseCase
import de.dkb.api.codeChallenge.infrastructure.adapter.kafka.dto.NotificationKafkaMessage
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Kafka adapter (driving adapter) for consuming notification events.
 * Implements hexagonal architecture by delegating to use cases.
 * Maintains compatibility with existing Kafka infrastructure.
 */
@Component
class NotificationKafkaAdapter(private val sendNotificationUseCase: SendNotificationUseCase) {

    /**
     * Consume notification messages from Kafka topic.
     * For the challenge this is deactivated, but demonstrates heavy use in real system.
     */
    @KafkaListener(
        topics = ["notifications"],
        groupId = "codechallenge_group",
        autoStartup = "\${kafka.listener.enabled:false}",
    )
    fun consumeNotification(message: NotificationKafkaMessage) {
        logger.info { "Kafka: Received notification message for user ${message.userId}" }

        try {
            val command = SendNotificationCommand.from(
                userId = message.userId,
                type = message.notificationType,
                message = message.message,
            )

            sendNotificationUseCase.execute(command)
            logger.debug { "Kafka: Successfully processed notification for user ${message.userId}" }
        } catch (e: Exception) {
            logger.error(e) { "Kafka: Failed to process notification for user ${message.userId}" }
        }
    }
}
