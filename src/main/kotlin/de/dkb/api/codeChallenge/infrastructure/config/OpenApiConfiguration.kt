package de.dkb.api.codeChallenge.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Notification Service API")
                .version("1.0.0")
                .description(
                    """
                        API for managing user notification subscriptions and sending notifications.

                        ## Features
                        - Register users with notification category subscriptions
                        - Send notifications to subscribed users
                        - Category-based subscription model

                        ## Architecture
                        This service implements Hexagonal Architecture (Ports & Adapters) with:
                        - Domain Layer: Business logic and entities
                        - Application Layer: Use cases
                        - Infrastructure Layer: REST/Kafka adapters, JPA persistence
                    """.trimIndent(),
                )
                .contact(
                    Contact()
                        .name("Development Team")
                        .email("ejcontrerasv@gmail.com"),
                )
                .license(
                    License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"),
                ),
        )
        .servers(
            listOf(
                Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"),
            ),
        )
}
