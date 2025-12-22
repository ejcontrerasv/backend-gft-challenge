package de.dkb.api.codeChallenge.infrastructure.adapter.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import de.dkb.api.codeChallenge.application.dto.NotificationResult
import de.dkb.api.codeChallenge.application.usecase.SendNotificationUseCase
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.SendNotificationRequest
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(NotificationController::class)
class NotificationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var sendNotificationUseCase: SendNotificationUseCase

    @Test
    fun `should send notification successfully`() {
        val userId = UUID.randomUUID()
        val request = SendNotificationRequest(
            userId = userId,
            notificationType = "type1",
            message = "Test message",
        )

        every { sendNotificationUseCase.execute(any()) } returns NotificationResult.Sent("Notification sent successfully")

        mockMvc.post("/notify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.sent") { value(true) }
            jsonPath("$.data.message") { value("Notification sent successfully") }
        }
    }

    @Test
    fun `should return ok when notification not sent due to subscription`() {
        val userId = UUID.randomUUID()
        val request = SendNotificationRequest(
            userId = userId,
            notificationType = "type1",
            message = "Test message",
        )

        every { sendNotificationUseCase.execute(any()) } returns NotificationResult.NotSent("User not subscribed")

        mockMvc.post("/notify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.sent") { value(false) }
            jsonPath("$.data.message") { value("User not subscribed") }
        }
    }

    @Test
    fun `should return not found when user does not exist`() {
        val userId = UUID.randomUUID()
        val request = SendNotificationRequest(
            userId = userId,
            notificationType = "type1",
            message = "Test message",
        )

        every { sendNotificationUseCase.execute(any()) } returns NotificationResult.UserNotFound("User not found")

        mockMvc.post("/notify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("User not found") }
        }
    }
}
