package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.User
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
import de.dkb.api.codeChallenge.infrastructure.persistence.adapter.UserRepositoryAdapter
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.LegacyNotificationType
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.entity.LegacyUserEntity
import de.dkb.api.codeChallenge.infrastructure.persistence.jpa.repository.LegacyUserJpaRepository
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

@DisplayName("NewWithFallbackReadStrategy")
class NewWithFallbackReadStrategyTest {

    private lateinit var newUserRepository: UserRepositoryAdapter
    private lateinit var legacyUserJpaRepository: LegacyUserJpaRepository
    private lateinit var legacyDataMigrator: LegacyDataMigrator
    private lateinit var strategy: NewWithFallbackReadStrategy

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
        legacyUserJpaRepository = mockk()
        legacyDataMigrator = mockk()
        strategy = NewWithFallbackReadStrategy(
            newUserRepository,
            legacyUserJpaRepository,
            legacyDataMigrator,
        )
    }

    @Nested
    @DisplayName("Strategy Type")
    inner class StrategyType {
        @Test
        @DisplayName("should have NEW_WITH_FALLBACK as strategy type")
        fun shouldHaveNewWithFallbackType() {
            assertEquals(ReadSource.NEW_WITH_FALLBACK, strategy.type)
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        @DisplayName("should return user from new repository when found")
        fun shouldReturnUserFromNewRepositoryWhenFound() {
            // Given
            val existingUser = createTestUser(testUserId, setOf(categoryA))
            every { newUserRepository.findById(UserId(testUserId)) } returns existingUser

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNotNull(result)
            assertEquals(testUserId, result!!.id.value)
            verify(exactly = 1) { newUserRepository.findById(any()) }
            verify(exactly = 0) { legacyUserJpaRepository.findById(any()) }
        }

        @Test
        @DisplayName("should fallback to legacy and migrate when user not found in new repository")
        fun shouldFallbackToLegacyAndMigrateWhenNotFoundInNew() {
            // Given
            val legacyEntity = LegacyUserEntity(
                id = testUserId,
                notifications = mutableSetOf(LegacyNotificationType.type1, LegacyNotificationType.type2),
            )
            val subscriptions = setOf(
                CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            )

            every { newUserRepository.findById(UserId(testUserId)) } returns null
            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.of(legacyEntity)
            every {
                legacyDataMigrator.migrateUserTypes(
                    UserId(testUserId),
                    "type1;type2",
                )
            } returns subscriptions
            every { newUserRepository.save(any()) } returns createTestUser(testUserId, setOf(categoryA))

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNotNull(result)
            assertEquals(testUserId, result!!.id.value)
            verify(exactly = 1) { newUserRepository.findById(any()) }
            verify(exactly = 1) { legacyUserJpaRepository.findById(testUserId) }
            verify(exactly = 1) { legacyDataMigrator.migrateUserTypes(any(), any()) }
            verify(exactly = 1) { newUserRepository.save(any()) } // Migration saves to new repo
        }

        @Test
        @DisplayName("should return null when user not found in both repositories")
        fun shouldReturnNullWhenNotFoundInBoth() {
            // Given
            every { newUserRepository.findById(UserId(testUserId)) } returns null
            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.empty()

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNull(result)
            verify(exactly = 1) { newUserRepository.findById(any()) }
            verify(exactly = 1) { legacyUserJpaRepository.findById(testUserId) }
            verify(exactly = 0) { legacyDataMigrator.migrateUserTypes(any(), any()) }
        }

        @Test
        @DisplayName("should return null when migration produces no subscriptions")
        fun shouldReturnNullWhenMigrationProducesNoSubscriptions() {
            // Given
            val legacyEntity = LegacyUserEntity(
                id = testUserId,
                notifications = mutableSetOf(LegacyNotificationType.type1),
            )

            every { newUserRepository.findById(UserId(testUserId)) } returns null
            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.of(legacyEntity)
            every { legacyDataMigrator.migrateUserTypes(any(), any()) } returns emptySet()

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNull(result)
            verify(exactly = 0) { newUserRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("existsById")
    inner class ExistsById {

        @Test
        @DisplayName("should return true when user exists in new repository")
        fun shouldReturnTrueWhenExistsInNew() {
            // Given
            every { newUserRepository.existsById(UserId(testUserId)) } returns true

            // When
            val result = strategy.existsById(UserId(testUserId))

            // Then
            assertTrue(result)
            verify(exactly = 1) { newUserRepository.existsById(any()) }
            // Should short-circuit - not check legacy
        }

        @Test
        @DisplayName("should return true when user exists only in legacy repository")
        fun shouldReturnTrueWhenExistsOnlyInLegacy() {
            // Given
            every { newUserRepository.existsById(UserId(testUserId)) } returns false
            every { legacyUserJpaRepository.existsById(testUserId) } returns true

            // When
            val result = strategy.existsById(UserId(testUserId))

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("should return false when user does not exist in either repository")
        fun shouldReturnFalseWhenNotExistsInEither() {
            // Given
            every { newUserRepository.existsById(UserId(testUserId)) } returns false
            every { legacyUserJpaRepository.existsById(testUserId) } returns false

            // When
            val result = strategy.existsById(UserId(testUserId))

            // Then
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("count")
    inner class Count {

        @Test
        @DisplayName("should count unique users from both repositories")
        fun shouldCountUniqueUsersFromBothRepositories() {
            // Given
            val user1Id = UUID.randomUUID()
            val user2Id = UUID.randomUUID()
            val user3Id = UUID.randomUUID()

            val newUsers = listOf(
                createTestUser(user1Id, setOf(categoryA)),
                createTestUser(user2Id, setOf(categoryB)),
            )
            val legacyUsers = listOf(
                LegacyUserEntity(user2Id, mutableSetOf(LegacyNotificationType.type4)), // Duplicate
                LegacyUserEntity(user3Id, mutableSetOf(LegacyNotificationType.type1)), // New
            )

            every { newUserRepository.findAll() } returns newUsers
            every { legacyUserJpaRepository.findAll() } returns legacyUsers

            // When
            val result = strategy.count()

            // Then
            assertEquals(3L, result) // user1, user2, user3 (deduplicated)
        }

        @Test
        @DisplayName("should return count from new only when legacy is empty")
        fun shouldReturnCountFromNewOnlyWhenLegacyEmpty() {
            // Given
            val newUsers = listOf(
                createTestUser(UUID.randomUUID(), setOf(categoryA)),
                createTestUser(UUID.randomUUID(), setOf(categoryB)),
            )

            every { newUserRepository.findAll() } returns newUsers
            every { legacyUserJpaRepository.findAll() } returns emptyList()

            // When
            val result = strategy.count()

            // Then
            assertEquals(2L, result)
        }
    }

    @Nested
    @DisplayName("findAll")
    inner class FindAll {

        @Test
        @DisplayName("should return users from new repository and migrate legacy users not in new")
        fun shouldReturnUsersFromNewAndMigrateLegacyNotInNew() {
            // Given
            val user1Id = UUID.randomUUID()
            val user2Id = UUID.randomUUID()
            val user3Id = UUID.randomUUID()

            val newUsers = listOf(
                createTestUser(user1Id, setOf(categoryA)),
            )
            val legacyUsers = listOf(
                LegacyUserEntity(user1Id, mutableSetOf(LegacyNotificationType.type1)), // Already in new - skip
                LegacyUserEntity(user2Id, mutableSetOf(LegacyNotificationType.type4)), // Not in new - migrate
                LegacyUserEntity(user3Id, mutableSetOf(LegacyNotificationType.type5)), // Not in new - migrate
            )

            val subscriptionB = setOf(
                CategorySubscription(category = categoryB, subscribedAt = Instant.now(), active = true),
            )

            every { newUserRepository.findAll() } returns newUsers
            every { legacyUserJpaRepository.findAll() } returns legacyUsers
            every { legacyDataMigrator.migrateUserTypes(any(), any()) } returns subscriptionB
            every { newUserRepository.save(any()) } answers { firstArg() }

            // When
            val result = strategy.findAll()

            // Then
            assertEquals(3, result.size)
            assertTrue(result.any { it.id.value == user1Id })
            assertTrue(result.any { it.id.value == user2Id })
            assertTrue(result.any { it.id.value == user3Id })

            // Verify migration was called for users not in new (user2 and user3)
            verify(exactly = 2) { legacyDataMigrator.migrateUserTypes(any(), any()) }
            verify(exactly = 2) { newUserRepository.save(any()) }
        }

        @Test
        @DisplayName("should skip legacy users that fail migration")
        fun shouldSkipLegacyUsersWithFailedMigration() {
            // Given
            val user1Id = UUID.randomUUID()
            val user2Id = UUID.randomUUID()

            val legacyUsers = listOf(
                LegacyUserEntity(user1Id, mutableSetOf(LegacyNotificationType.type1)),
                LegacyUserEntity(user2Id, mutableSetOf(LegacyNotificationType.type4)),
            )

            val subscriptionA = setOf(
                CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            )

            every { newUserRepository.findAll() } returns emptyList()
            every { legacyUserJpaRepository.findAll() } returns legacyUsers
            every {
                legacyDataMigrator.migrateUserTypes(UserId(user1Id), any())
            } returns subscriptionA
            every {
                legacyDataMigrator.migrateUserTypes(UserId(user2Id), any())
            } throws RuntimeException("Migration failed")
            every { newUserRepository.save(any()) } answers { firstArg() }

            // When
            val result = strategy.findAll()

            // Then
            assertEquals(1, result.size)
            assertEquals(user1Id, result.first().id.value)
        }
    }

    @Nested
    @DisplayName("Migration Behavior")
    inner class MigrationBehavior {

        @Test
        @DisplayName("should perform on-the-fly migration and persist to new repository")
        fun shouldPerformOnTheFlyMigrationAndPersist() {
            // Given
            val legacyEntity = LegacyUserEntity(
                id = testUserId,
                notifications = mutableSetOf(LegacyNotificationType.type1, LegacyNotificationType.type5),
            )
            val subscriptions = setOf(
                CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
                CategorySubscription(category = categoryB, subscribedAt = Instant.now(), active = true),
            )

            every { newUserRepository.findById(UserId(testUserId)) } returns null
            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.of(legacyEntity)
            every {
                legacyDataMigrator.migrateUserTypes(UserId(testUserId), "type1;type5")
            } returns subscriptions
            every { newUserRepository.save(any()) } answers { firstArg() }

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNotNull(result)
            assertEquals(2, result!!.subscriptions.size)
            assertTrue(result.subscriptions.any { it.category.id.value == "CATEGORY_A" })
            assertTrue(result.subscriptions.any { it.category.id.value == "CATEGORY_B" })

            // Verify the user was saved to the new repository (migration persisted)
            verify(exactly = 1) {
                newUserRepository.save(
                    match { user ->
                        user.id.value == testUserId && user.subscriptions.size == 2
                    },
                )
            }
        }

        @Test
        @DisplayName("should use LegacyDataMigrator for migration (unlike LEGACY_ONLY)")
        fun shouldUseLegacyDataMigratorForMigration() {
            // This test documents that NewWithFallbackReadStrategy DOES use LegacyDataMigrator
            // (unlike LegacyOnlyReadStrategy which uses CategoryResolutionService directly)

            // Given
            val legacyEntity = LegacyUserEntity(
                id = testUserId,
                notifications = mutableSetOf(LegacyNotificationType.type1),
            )
            val subscriptions = setOf(
                CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
            )

            every { newUserRepository.findById(UserId(testUserId)) } returns null
            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.of(legacyEntity)
            every { legacyDataMigrator.migrateUserTypes(any(), any()) } returns subscriptions
            every { newUserRepository.save(any()) } answers { firstArg() }

            // When
            strategy.findById(UserId(testUserId))

            // Then
            // Verify LegacyDataMigrator was called (this logs "Migrating user..." messages)
            verify(exactly = 1) { legacyDataMigrator.migrateUserTypes(UserId(testUserId), "type1") }

            // And the migrated user was persisted to the new repository
            verify(exactly = 1) { newUserRepository.save(any()) }
        }
    }
}
