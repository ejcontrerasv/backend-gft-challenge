package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
import de.dkb.api.codeChallenge.infrastructure.persistence.adapter.UserRepositoryAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

@DisplayName("NewOnlyReadStrategy")
class NewOnlyReadStrategyTest {

    private lateinit var newUserRepository: UserRepositoryAdapter
    private lateinit var strategy: NewOnlyReadStrategy

    private val testUserId = UUID.randomUUID()
    private val categoryA = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
            NotificationType("type2", CategoryId("CATEGORY_A"), Instant.now()),
            NotificationType("type3", CategoryId("CATEGORY_A"), Instant.now()),
        ),
    )
    private val categoryB = NotificationCategory(
        id = CategoryId("CATEGORY_B"),
        name = "Category B",
        types = setOf(
            NotificationType("type4", CategoryId("CATEGORY_B"), Instant.now()),
            NotificationType("type5", CategoryId("CATEGORY_B"), Instant.now()),
        ),
    )

    private fun createTestUser(id: UUID, categories: Set<NotificationCategory>): User {
        val subscriptions = categories.map { category ->
            CategorySubscription(
                category = category,
                subscribedAt = Instant.now(),
                active = true,
            )
        }.toSet()
        return User(id = UserId(id), subscriptions = subscriptions)
    }

    @BeforeEach
    fun setUp() {
        newUserRepository = mockk()
        strategy = NewOnlyReadStrategy(newUserRepository)
    }

    @Nested
    @DisplayName("Strategy Type")
    inner class StrategyType {
        @Test
        @DisplayName("should have NEW_ONLY as strategy type")
        fun shouldHaveNewOnlyType() {
            assertEquals(ReadSource.NEW_ONLY, strategy.type)
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        @DisplayName("should return user from new repository when found")
        fun shouldReturnUserFromNewRepositoryWhenFound() {
            // Given
            val existingUser = createTestUser(testUserId, setOf(categoryA, categoryB))
            every { newUserRepository.findById(UserId(testUserId)) } returns existingUser

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNotNull(result)
            assertEquals(testUserId, result!!.id.value)
            assertEquals(2, result.subscriptions.size)
            verify(exactly = 1) { newUserRepository.findById(UserId(testUserId)) }
        }

        @Test
        @DisplayName("should return null when user not found in new repository - NO fallback to legacy")
        fun shouldReturnNullWhenNotFoundInNew() {
            // Given
            every { newUserRepository.findById(UserId(testUserId)) } returns null

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNull(result)
            verify(exactly = 1) { newUserRepository.findById(UserId(testUserId)) }
            // Note: NO legacy repository is checked - this is the key difference from NEW_WITH_FALLBACK
        }

        @Test
        @DisplayName("should return user with subscriptions intact")
        fun shouldReturnUserWithSubscriptionsIntact() {
            // Given
            val existingUser = createTestUser(testUserId, setOf(categoryA))
            every { newUserRepository.findById(UserId(testUserId)) } returns existingUser

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNotNull(result)
            assertTrue(result!!.subscriptions.any { it.category.id.value == "CATEGORY_A" })
            assertTrue(result.canReceiveNotificationType("type1"))
            assertTrue(result.canReceiveNotificationType("type2"))
            assertTrue(result.canReceiveNotificationType("type3"))
        }
    }

    @Nested
    @DisplayName("existsById")
    inner class ExistsById {

        @Test
        @DisplayName("should check existence only in new repository")
        fun shouldCheckExistenceOnlyInNewRepository() {
            // Given
            every { newUserRepository.existsById(UserId(testUserId)) } returns true

            // When
            val result = strategy.existsById(UserId(testUserId))

            // Then
            assertTrue(result)
            verify(exactly = 1) { newUserRepository.existsById(UserId(testUserId)) }
        }

        @Test
        @DisplayName("should return false when user does not exist in new repository - NO fallback")
        fun shouldReturnFalseWhenNotExistsInNew() {
            // Given
            every { newUserRepository.existsById(UserId(testUserId)) } returns false

            // When
            val result = strategy.existsById(UserId(testUserId))

            // Then
            assertFalse(result)
            // Note: NO legacy repository is checked
        }
    }

    @Nested
    @DisplayName("count")
    inner class Count {

        @Test
        @DisplayName("should count users only from new repository")
        fun shouldCountUsersOnlyFromNewRepository() {
            // Given
            val users = listOf(
                createTestUser(UUID.randomUUID(), setOf(categoryA)),
                createTestUser(UUID.randomUUID(), setOf(categoryB)),
                createTestUser(UUID.randomUUID(), setOf(categoryA, categoryB)),
            )
            every { newUserRepository.findAll() } returns users

            // When
            val result = strategy.count()

            // Then
            assertEquals(3L, result)
            verify(exactly = 1) { newUserRepository.findAll() }
        }

        @Test
        @DisplayName("should return zero when new repository is empty")
        fun shouldReturnZeroWhenNewRepositoryEmpty() {
            // Given
            every { newUserRepository.findAll() } returns emptyList()

            // When
            val result = strategy.count()

            // Then
            assertEquals(0L, result)
        }
    }

    @Nested
    @DisplayName("findAll")
    inner class FindAll {

        @Test
        @DisplayName("should return all users from new repository only")
        fun shouldReturnAllUsersFromNewRepositoryOnly() {
            // Given
            val user1Id = UUID.randomUUID()
            val user2Id = UUID.randomUUID()
            val users = listOf(
                createTestUser(user1Id, setOf(categoryA)),
                createTestUser(user2Id, setOf(categoryB)),
            )
            every { newUserRepository.findAll() } returns users

            // When
            val result = strategy.findAll()

            // Then
            assertEquals(2, result.size)
            assertTrue(result.any { it.id.value == user1Id })
            assertTrue(result.any { it.id.value == user2Id })
            verify(exactly = 1) { newUserRepository.findAll() }
        }

        @Test
        @DisplayName("should return empty list when new repository is empty")
        fun shouldReturnEmptyListWhenNewRepositoryEmpty() {
            // Given
            every { newUserRepository.findAll() } returns emptyList()

            // When
            val result = strategy.findAll()

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("No Migration Behavior")
    inner class NoMigrationBehavior {

        @Test
        @DisplayName("should NOT perform any migration - uses new repository only")
        fun shouldNotPerformAnyMigration() {
            // This test documents that NewOnlyReadStrategy does NOT perform migration.
            // It only reads from the new repository.
            // Legacy users that haven't been migrated will simply not be found (404).

            // Given
            every { newUserRepository.findById(UserId(testUserId)) } returns null
            every { newUserRepository.findAll() } returns emptyList()
            every { newUserRepository.existsById(UserId(testUserId)) } returns false

            // When
            val findByIdResult = strategy.findById(UserId(testUserId))
            val findAllResult = strategy.findAll()
            val existsResult = strategy.existsById(UserId(testUserId))

            // Then
            assertNull(findByIdResult)
            assertTrue(findAllResult.isEmpty())
            assertFalse(existsResult)

            // Verify ONLY newUserRepository was called - no legacy, no migration
            verify(exactly = 1) { newUserRepository.findById(any()) }
            verify(exactly = 1) { newUserRepository.findAll() }
            verify(exactly = 1) { newUserRepository.existsById(any()) }
        }

        @Test
        @DisplayName("should be faster than NEW_WITH_FALLBACK - single DB query")
        fun shouldBeFasterWithSingleDbQuery() {
            // This test documents the performance benefit of NEW_ONLY:
            // Only one repository is queried, no fallback logic.

            // Given
            val existingUser = createTestUser(testUserId, setOf(categoryA))
            every { newUserRepository.findById(UserId(testUserId)) } returns existingUser

            // When
            strategy.findById(UserId(testUserId))

            // Then
            // Only 1 call to newUserRepository (vs 2 in NEW_WITH_FALLBACK when user not in new)
            verify(exactly = 1) { newUserRepository.findById(any()) }
        }
    }

    @Nested
    @DisplayName("Post-Migration Usage")
    inner class PostMigrationUsage {

        @Test
        @DisplayName("should be used after 100% migration is complete")
        fun shouldBeUsedAfterMigrationComplete() {
            // This test documents when to use NEW_ONLY:
            // After all users have been migrated from legacy to new schema.

            // Given - All users exist in new repository
            val user1 = createTestUser(UUID.randomUUID(), setOf(categoryA))
            val user2 = createTestUser(UUID.randomUUID(), setOf(categoryB))
            val user3 = createTestUser(UUID.randomUUID(), setOf(categoryA, categoryB))

            every { newUserRepository.findAll() } returns listOf(user1, user2, user3)
            every { newUserRepository.findById(user1.id) } returns user1
            every { newUserRepository.findById(user2.id) } returns user2
            every { newUserRepository.findById(user3.id) } returns user3

            // When
            val allUsers = strategy.findAll()
            val foundUser1 = strategy.findById(user1.id)
            val foundUser2 = strategy.findById(user2.id)
            val foundUser3 = strategy.findById(user3.id)

            // Then - All users are accessible from new repository
            assertEquals(3, allUsers.size)
            assertNotNull(foundUser1)
            assertNotNull(foundUser2)
            assertNotNull(foundUser3)

            // Benefits:
            // - Best performance (single DB query)
            // - No migration overhead
            // - Legacy table can be safely archived/dropped
        }
    }
}
