package de.dkb.api.codeChallenge.infrastructure.adapter.rest

import de.dkb.api.codeChallenge.application.dto.RegisterUserCommand
import de.dkb.api.codeChallenge.application.dto.UserRegistrationResult
import de.dkb.api.codeChallenge.application.usecase.RegisterUserUseCase
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.ApiResponse
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.RegisterUserRequest
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.RegisterUserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

private val logger = KotlinLogging.logger {}

/**
 * REST adapter for user registration operations.
 * Single Responsibility: handles only user registration.
 */
@RestController
@RequestMapping
@Tag(name = "User Registration", description = "API for user registration and subscription management")
class UserRegistrationController(private val registerUserUseCase: RegisterUserUseCase) {

    /**
     * Register user with notification subscriptions.
     * Backward compatible: accepts notification types, converts to categories internally.
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
    fun registerUser(@Valid @RequestBody request: RegisterUserRequest): ResponseEntity<ApiResponse<RegisterUserResponse>> {
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
                ResponseEntity.ok(ApiResponse.success(response, "User registered successfully"))
            }

            is UserRegistrationResult.Failure -> {
                val errorResponse = ApiResponse.error(
                    message = "Registration failed",
                    errors = result.errors,
                )
                ResponseEntity.badRequest().body(errorResponse)
            }
        }
    }
}
