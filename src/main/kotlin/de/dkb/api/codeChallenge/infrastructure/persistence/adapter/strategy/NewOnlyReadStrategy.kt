package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
import de.dkb.api.codeChallenge.infrastructure.persistence.adapter.UserRepositoryAdapter
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class NewOnlyReadStrategy(private val newUserRepository: UserRepositoryAdapter) : UserReadStrategy {

    override val type: ReadSource = ReadSource.NEW_ONLY

    override fun findById(id: UserId): User? {
        logger.debug { "Read strategy: NEW_ONLY for user ${id.value}" }
        return newUserRepository.findById(id)
    }

    override fun existsById(id: UserId): Boolean = newUserRepository.existsById(id)

    override fun count(): Long = newUserRepository.findAll().size.toLong()

    override fun findAll(): List<User> = newUserRepository.findAll()
}
