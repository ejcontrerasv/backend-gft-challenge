package de.dkb.api.codeChallenge.infrastructure.adapter.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import de.dkb.api.codeChallenge.application.dto.UserRegistrationResult
import de.dkb.api.codeChallenge.application.usecase.RegisterUserUseCase
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.RegisterUserRequest
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(UserRegistrationController::class)
class UserRegistrationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Test
    fun `should register user successfully`() {
        val userId = UUID.randomUUID()
        val request = RegisterUserRequest(id = userId, notifications = listOf("type1", "type2"))

        every { registerUserUseCase.execute(any()) } returns UserRegistrationResult.Success(
            userId = UserId(userId),
            subscribedCategories = setOf("CATEGORY_A"),
        )

        mockMvc.post("/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.userId") { value(userId.toString()) }
            jsonPath("$.data.subscribedCategories[0]") { value("CATEGORY_A") }
            jsonPath("$.message") { value("User registered successfully") }
        }
    }

    @Test
    fun `should return bad request when registration fails`() {
        val userId = UUID.randomUUID()
        val request = RegisterUserRequest(id = userId, notifications = listOf("invalid"))

        every { registerUserUseCase.execute(any()) } returns UserRegistrationResult.Failure(
            userId = UserId(userId),
            errors = listOf("No valid notification types"),
        )

        mockMvc.post("/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Registration failed") }
        }
    }
}
