package de.dkb.api.codeChallenge.infrastructure.persistence.adapter.strategy

import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.service.CategoryResolutionService
import de.dkb.api.codeChallenge.infrastructure.config.ReadSource
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

@DisplayName("LegacyOnlyReadStrategy")
class LegacyOnlyReadStrategyTest {

    private lateinit var legacyUserJpaRepository: LegacyUserJpaRepository
    private lateinit var categoryResolutionService: CategoryResolutionService
    private lateinit var strategy: LegacyOnlyReadStrategy

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

    @BeforeEach
    fun setUp() {
        legacyUserJpaRepository = mockk()
        categoryResolutionService = mockk()
        strategy = LegacyOnlyReadStrategy(legacyUserJpaRepository, categoryResolutionService)
    }

    @Nested
    @DisplayName("Strategy Type")
    inner class StrategyType {
        @Test
        @DisplayName("should have LEGACY_ONLY as strategy type")
        fun shouldHaveLegacyOnlyType() {
            assertEquals(ReadSource.LEGACY_ONLY, strategy.type)
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        @DisplayName("should find user from legacy table and convert without migration")
        fun shouldFindUserFromLegacyAndConvertWithoutMigration() {
            // Given
            val legacyEntity = LegacyUserEntity(
                id = testUserId,
                notifications = mutableSetOf(LegacyNotificationType.type1, LegacyNotificationType.type2),
            )

            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.of(legacyEntity)
            every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1", "type2")) } returns setOf(categoryA)

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNotNull(result)
            assertEquals(testUserId, result!!.id.value)
            assertEquals(1, result.subscriptions.size)
            assertTrue(result.subscriptions.any { it.category.id.value == "CATEGORY_A" })

            // Verify NO migration was triggered (CategoryResolutionService is read-only)
            verify(exactly = 1) { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) }
        }

        @Test
        @DisplayName("should return null when user not found in legacy table")
        fun shouldReturnNullWhenUserNotFound() {
            // Given
            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.empty()

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("should return null when no categories can be resolved")
        fun shouldReturnNullWhenNoCategoriesResolved() {
            // Given
            val legacyEntity = LegacyUserEntity(
                id = testUserId,
                notifications = mutableSetOf(LegacyNotificationType.type1),
            )

            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.of(legacyEntity)
            every { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) } returns emptySet()

            // When
            val result = strategy.findById(UserId(testUserId))

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("existsById")
    inner class ExistsById {

        @Test
        @DisplayName("should check existence in legacy table only")
        fun shouldCheckExistenceInLegacyTableOnly() {
            // Given
            every { legacyUserJpaRepository.existsById(testUserId) } returns true

            // When
            val result = strategy.existsById(UserId(testUserId))

            // Then
            assertTrue(result)
            verify(exactly = 1) { legacyUserJpaRepository.existsById(testUserId) }
        }

        @Test
        @DisplayName("should return false when user does not exist in legacy table")
        fun shouldReturnFalseWhenUserDoesNotExist() {
            // Given
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
        @DisplayName("should count users from legacy table only")
        fun shouldCountUsersFromLegacyTableOnly() {
            // Given
            val legacyUsers = listOf(
                LegacyUserEntity(UUID.randomUUID(), mutableSetOf(LegacyNotificationType.type1)),
                LegacyUserEntity(UUID.randomUUID(), mutableSetOf(LegacyNotificationType.type2)),
            )
            every { legacyUserJpaRepository.findAll() } returns legacyUsers

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
        @DisplayName("should find all users from legacy table and convert without migration")
        fun shouldFindAllUsersFromLegacyAndConvertWithoutMigration() {
            // Given
            val legacyUsers = listOf(
                LegacyUserEntity(UUID.randomUUID(), mutableSetOf(LegacyNotificationType.type1)),
                LegacyUserEntity(UUID.randomUUID(), mutableSetOf(LegacyNotificationType.type4)),
            )
            every { legacyUserJpaRepository.findAll() } returns legacyUsers
            every { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) } returns setOf(categoryA)

            // When
            val result = strategy.findAll()

            // Then
            assertEquals(2, result.size)

            // Verify CategoryResolutionService was called for each user (read-only conversion)
            verify(exactly = 2) { categoryResolutionService.resolveCategoriesFromLegacyTypes(any()) }
        }

        @Test
        @DisplayName("should skip users that fail conversion")
        fun shouldSkipUsersWithFailedConversion() {
            // Given
            val user1Id = UUID.randomUUID()
            val user2Id = UUID.randomUUID()
            val legacyUsers = listOf(
                LegacyUserEntity(user1Id, mutableSetOf(LegacyNotificationType.type1)),
                LegacyUserEntity(user2Id, mutableSetOf(LegacyNotificationType.type4)),
            )
            every { legacyUserJpaRepository.findAll() } returns legacyUsers
            every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1")) } returns setOf(categoryA)
            every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type4")) } returns emptySet()

            // When
            val result = strategy.findAll()

            // Then
            assertEquals(1, result.size)
            assertEquals(user1Id, result.first().id.value)
        }
    }

    @Nested
    @DisplayName("No Migration Guarantee")
    inner class NoMigrationGuarantee {

        @Test
        @DisplayName("should NOT use LegacyDataMigrator - uses CategoryResolutionService instead")
        fun shouldNotUseLegacyDataMigrator() {
            // This test documents that LegacyOnlyReadStrategy does NOT depend on LegacyDataMigrator.
            // The strategy uses CategoryResolutionService directly which is a read-only operation.

            // Given
            val legacyEntity = LegacyUserEntity(
                id = testUserId,
                notifications = mutableSetOf(LegacyNotificationType.type1),
            )

            every { legacyUserJpaRepository.findById(testUserId) } returns Optional.of(legacyEntity)
            every { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1")) } returns setOf(categoryA)

            // When
            strategy.findById(UserId(testUserId))

            // Then
            // CategoryResolutionService.resolveCategoriesFromLegacyTypes is a read-only method
            // that resolves categories without persisting anything to the database.
            // This is different from LegacyDataMigrator.migrateUserTypes which logs migration
            // messages and could trigger persistence.
            verify(exactly = 1) { categoryResolutionService.resolveCategoriesFromLegacyTypes(setOf("type1")) }

            // Note: LegacyDataMigrator is NOT injected into this strategy at all,
            // so it's impossible for it to be called.
        }
    }
}
