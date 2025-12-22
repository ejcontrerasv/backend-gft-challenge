package de.dkb.api.codeChallenge.infrastructure.adapter.rest

import de.dkb.api.codeChallenge.application.dto.NotificationResult
import de.dkb.api.codeChallenge.application.dto.SendNotificationCommand
import de.dkb.api.codeChallenge.application.usecase.SendNotificationUseCase
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.ApiResponse
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.SendNotificationRequest
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.SendNotificationResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

private val logger = KotlinLogging.logger {}

/**
 * REST adapter for notification operations.
 * Single Responsibility: handles only notification sending.
 */
@RestController
@RequestMapping
@Tag(name = "Notifications", description = "API for sending notifications to users")
class NotificationController(private val sendNotificationUseCase: SendNotificationUseCase) {

    /**
     * Send notification to user.
     * Only sends if user is subscribed to the notification type's category.
     */
    @Operation(
        summary = "Send notification to user",
        description = "Sends a notification to a user if they are subscribed to the notification type's category.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "Notification processed (sent or not sent based on subscription)",
            ),
            SwaggerApiResponse(
                responseCode = "404",
                description = "User not found",
            ),
        ],
    )
    @PostMapping("/notify")
    fun sendNotification(@Valid @RequestBody request: SendNotificationRequest): ResponseEntity<ApiResponse<SendNotificationResponse>> {
        logger.info { "REST: Send notification type '${request.notificationType}' to user ${request.userId}" }

        val command = SendNotificationCommand.from(
            userId = request.userId,
            type = request.notificationType,
            message = request.message,
        )

        return when (val result = sendNotificationUseCase.execute(command)) {
            is NotificationResult.Sent -> {
                val response = SendNotificationResponse(
                    sent = true,
                    message = result.message,
                )
                ResponseEntity.ok(ApiResponse.success(response, request.message))
            }

            is NotificationResult.NotSent -> {
                val response = SendNotificationResponse(
                    sent = false,
                    message = result.reason,
                )
                ResponseEntity.ok(ApiResponse.success(response, result.reason))
            }

            is NotificationResult.UserNotFound -> {
                val errorResponse = ApiResponse.error<SendNotificationResponse>(
                    message = result.message,
                )
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
            }
        }
    }
}
