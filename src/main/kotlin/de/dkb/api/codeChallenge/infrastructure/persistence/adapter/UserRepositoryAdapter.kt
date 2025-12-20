package de.dkb.api.codeChallenge.infrastructure.persistence.adapter

import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.repository.CategoryConfigRepository
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.UserSubscriptionJpaRepository
import de.dkb.api.codeChallenge.infrastructure.persistence.mapper.UserSubscriptionMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Implementation of UserRepository using the new user_subscriptions table.
 * Reads from a category-based schema.
 */
@Component
class UserRepositoryAdapter(
    private val userSubscriptionJpaRepository: UserSubscriptionJpaRepository,
    private val categoryConfigRepository: CategoryConfigRepository,
) : UserRepository {

    override fun findById(id: UserId): User? {
        val subscriptionEntities = userSubscriptionJpaRepository.findByUserId(id.value)
        if (subscriptionEntities.isEmpty()) return null

        val categoryMap = categoryConfigRepository.findAllCategories().associateBy { it.id.value }
        return UserSubscriptionMapper.toDomain(id.value, subscriptionEntities, categoryMap)
    }

    @Transactional
    override fun save(user: User): User {
        logger.debug { "Saving user ${user.id.value} with ${user.subscriptions.size} subscriptions" }

        userSubscriptionJpaRepository.deleteByUserId(user.id.value)
        val entities = UserSubscriptionMapper.toEntities(user)
        userSubscriptionJpaRepository.saveAll(entities)

        logger.info { "Saved user ${user.id.value} successfully" }
        return user
    }

    override fun findByCategory(categoryId: CategoryId): List<User> {
        val subscriptionEntities = userSubscriptionJpaRepository.findByCategoryId(categoryId.value)
        val userIdsToSubscriptions = subscriptionEntities.groupBy { it.userId }
        val categoryMap = categoryConfigRepository.findAllCategories().associateBy { it.id.value }

        return userIdsToSubscriptions.map { (userId, subscriptions) ->
            UserSubscriptionMapper.toDomain(userId, subscriptions, categoryMap)
        }
    }

    override fun existsById(id: UserId): Boolean = userSubscriptionJpaRepository.findByUserId(id.value).isNotEmpty()

    override fun count(): Long = userSubscriptionJpaRepository.findAll().map { it.userId }.distinct().size.toLong()

    override fun findAll(): List<User> {
        val allSubscriptions = userSubscriptionJpaRepository.findAll()
        val userIdsToSubscriptions = allSubscriptions.groupBy { it.userId }
        val categoryMap = categoryConfigRepository.findAllCategories().associateBy { it.id.value }

        return userIdsToSubscriptions.map { (userId, subscriptions) ->
            UserSubscriptionMapper.toDomain(userId, subscriptions, categoryMap)
        }
    }
}
