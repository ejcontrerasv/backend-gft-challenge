package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * API request DTO for user registration.
 * Maintains backward compatibility with existing API contract.
 */
@Schema(description = "Request to register a user with notification subscriptions")
data class RegisterUserRequest(
    @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: UUID,

    @Schema(description = "List of notification types to subscribe", example = "[\"type1\", \"type2\", \"type3\"]")
    val notifications: List<String>,
)
