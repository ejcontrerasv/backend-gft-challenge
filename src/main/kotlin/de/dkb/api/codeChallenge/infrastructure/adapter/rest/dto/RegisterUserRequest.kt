package de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto

import java.util.UUID

/**
 * API request DTO for user registration.
 * Maintains backward compatibility with existing API contract.
 */
data class RegisterUserRequest(
    val id: UUID,
    val notifications: List<String>
)
