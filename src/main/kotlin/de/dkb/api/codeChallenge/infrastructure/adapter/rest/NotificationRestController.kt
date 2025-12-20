package de.dkb.api.codeChallenge.infrastructure.adapter.rest

import de.dkb.api.codeChallenge.application.dto.NotificationResult
import de.dkb.api.codeChallenge.application.dto.RegisterUserCommand
import de.dkb.api.codeChallenge.application.dto.SendNotificationCommand
import de.dkb.api.codeChallenge.application.dto.UserRegistrationResult
import de.dkb.api.codeChallenge.application.usecase.RegisterUserUseCase
import de.dkb.api.codeChallenge.application.usecase.SendNotificationUseCase
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

private val logger = KotlinLogging.logger {}

/**
 * REST adapter (driving adapter) for notification operations.
 * Implements hexagonal architecture by delegating to use cases.
 * Maintains backward compatibility with existing API contract.
 */
@RestController
@Tag(name = "Notifications", description = "API for user registration and notification management")
class NotificationRestController(private val registerUserUseCase: RegisterUserUseCase, private val sendNotificationUseCase: SendNotificationUseCase) {

    /**
     * Register user with notification subscriptions.
     * Backward compatible: accepts notification types, converts to categories internally.
     *
     * Example: POST /register
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440000",
     *   "notifications": ["type1", "type2", "type3"]
     * }
     */
    @Operation(
        summary = "Register user with notification subscriptions",
        description = "Registers a user with notification type subscriptions. Types are automatically converted to categories.",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(
                responseCode = "200",
                description = "User registered successfully",
                content = [Content(schema = Schema(implementation = RegisterUserResponse::class))],
            ),
            SwaggerApiResponse(
                responseCode = "400",
                description = "Invalid request or validation failed",
            ),
        ],
    )
    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegisterUserRequest): ResponseEntity<ApiResponse<RegisterUserResponse>> {
        logger.info { "REST: Register user ${request.id} with types: ${request.notifications}" }

        val command = RegisterUserCommand.from(
            userId = request.id,
            types = request.notifications.toSet(),
        )

        return when (val result = registerUserUseCase.execute(command)) {
            is UserRegistrationResult.Success -> {
                val response = RegisterUserResponse(
                    userId = result.userId.value.toString(),
                    subscribedCategories = result.subscribedCategories.toList(),
                )
                ResponseEntity.ok(ApiResponse.Success(response, "User registered successfully"))
            }

            is UserRegistrationResult.Failure -> {
                val errorResponse = ApiResponse.Error(
                    message = "Registration failed",
                    errors = result.errors,
                )
                ResponseEntity.badRequest().body(errorResponse)
            }
        }
    }

    /**
     * Send notification to user.
     * Only sends if user is subscribed to the notification type's category.
     *
     * Example: POST /notify
     * {
     *   "userId": "550e8400-e29b-41d4-a716-446655440000",
     *   "notificationType": "type1",
     *   "message": "Your order has shipped!"
     * }
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
    fun sendNotification(@RequestBody request: SendNotificationRequest): ResponseEntity<ApiResponse<SendNotificationResponse>> {
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
                ResponseEntity.ok(ApiResponse.Success(response, request.message))
            }

            is NotificationResult.NotSent -> {
                val response = SendNotificationResponse(
                    sent = false,
                    message = result.reason,
                )
                ResponseEntity.ok(ApiResponse.Success(response, result.reason))
            }

            is NotificationResult.UserNotFound -> {
                val errorResponse = ApiResponse.Error(
                    message = result.message,
                )
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
            }
        }
    }
}
