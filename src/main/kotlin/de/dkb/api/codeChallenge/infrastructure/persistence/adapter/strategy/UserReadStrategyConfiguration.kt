package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserReadStrategyConfiguration {

    @Bean
    fun userReadStrategiesMap(
        strategies: List<UserReadStrategy>,
    ): Map<ReadSource, UserReadStrategy> = strategies.associateBy { it.type }
}
