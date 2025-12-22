package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * API request DTO for user registration.
 * Maintains backward compatibility with existing API contract.
 */
@Schema(description = "Request to register a user with notification subscriptions")
data class RegisterUserRequest(
    @field:NotNull(message = "User ID is required")
    @field:Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,

    @field:NotEmpty(message = "Notifications list cannot be empty")
    @field:Size(min = 1, max = 20, message = "Notifications must contain between 1 and 20 items")
    @field:Schema(description = "List of notification types to subscribe", example = "[\"type1\", \"type2\", \"type3\"]")
    val notifications: List<@field:Size(min = 1, message = "Notification type cannot be empty") String>,
)
