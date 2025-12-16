package de.dkb.api.codeChallenge.integration

import com.fasterxml.jackson.databind.ObjectMapper
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.RegisterUserRequest
import de.dkb.api.codeChallenge.infrastructure.adapter.rest.dto.SendNotificationRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = true)
@ActiveProfiles("test")
class NotificationApiIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `register and send notification end-to-end`() {
        val userId = UUID.randomUUID()
        val registerReq = RegisterUserRequest(id = userId, notifications = listOf("type1", "type5"))

        mockMvc.post("/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(registerReq)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.userId") { value(userId.toString()) }
        }

        val notifyReq = SendNotificationRequest(userId = userId, notificationType = "type5", message = "hello")

        mockMvc.post("/notify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(notifyReq)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.sent") { value(true) }
        }
    }
}
