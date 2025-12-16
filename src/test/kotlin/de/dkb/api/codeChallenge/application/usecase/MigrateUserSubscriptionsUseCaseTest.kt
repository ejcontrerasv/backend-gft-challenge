package de.dkb.api.codeChallenge.application.usecase

import de.dkb.api.codeChallenge.application.dto.MigrateUserCommand
import de.dkb.api.codeChallenge.domain.model.CategorySubscription
import de.dkb.api.codeChallenge.domain.model.NotificationCategory
import de.dkb.api.codeChallenge.domain.model.NotificationType
import de.dkb.api.codeChallenge.domain.model.valueobject.CategoryId
import de.dkb.api.codeChallenge.domain.model.valueobject.UserId
import de.dkb.api.codeChallenge.domain.repository.UserRepository
import de.dkb.api.codeChallenge.domain.service.LegacyDataMigrator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MigrateUserSubscriptionsUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var legacyDataMigrator: LegacyDataMigrator
    private lateinit var useCase: MigrateUserSubscriptionsUseCase

    private val categoryA = NotificationCategory(
        id = CategoryId("CATEGORY_A"),
        name = "Category A",
        types = setOf(
            NotificationType("type1", CategoryId("CATEGORY_A"), Instant.now()),
        ),
    )

    @BeforeEach
    fun setup() {
        userRepository = mockk(relaxed = true)
        legacyDataMigrator = mockk()
        useCase = MigrateUserSubscriptionsUseCase(userRepository, legacyDataMigrator)
    }

    @Test
    fun `should migrate user successfully`() {
        val userId = UserId(UUID.randomUUID())
        val command = MigrateUserCommand(userId, "type1;type2")
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
        )

        every { legacyDataMigrator.migrateUserTypes(userId, "type1;type2") } returns subscriptions

        val result = useCase.execute(command)

        assertTrue(result is MigrationResult.Success)
        val success = result as MigrationResult.Success
        assertEquals(userId.value.toString(), success.userId)
        assertEquals(1, success.categoriesCount)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `should fail when no subscriptions found`() {
        val userId = UserId(UUID.randomUUID())
        val command = MigrateUserCommand(userId, "unknown")

        every { legacyDataMigrator.migrateUserTypes(userId, "unknown") } returns emptySet()

        val result = useCase.execute(command)

        assertTrue(result is MigrationResult.Failed)
        val failed = result as MigrationResult.Failed
        assertTrue(failed.reason.contains("No valid subscriptions"))
    }

    @Test
    fun `should handle exception during migration`() {
        val userId = UserId(UUID.randomUUID())
        val command = MigrateUserCommand(userId, "type1")

        every { legacyDataMigrator.migrateUserTypes(any(), any()) } throws RuntimeException("Migration error")

        val result = useCase.execute(command)

        assertTrue(result is MigrationResult.Failed)
        val failed = result as MigrationResult.Failed
        assertTrue(failed.reason.contains("Migration error"))
    }

    @Test
    fun `should execute batch migration`() {
        val userId1 = UserId(UUID.randomUUID())
        val userId2 = UserId(UUID.randomUUID())
        val commands = listOf(
            MigrateUserCommand(userId1, "type1"),
            MigrateUserCommand(userId2, "type2"),
        )
        val subscriptions = setOf(
            CategorySubscription(category = categoryA, subscribedAt = Instant.now(), active = true),
        )

        every { legacyDataMigrator.migrateUserTypes(any(), any()) } returns subscriptions

        val result = useCase.executeBatch(commands)

        assertEquals(2, result.total)
        assertEquals(2, result.succeeded)
        assertEquals(0, result.failed)
    }

    @Test
    fun `should create command from factory method`() {
        val uuid = UUID.randomUUID()
        val command = MigrateUserCommand.from(uuid, "type1;type2")

        assertEquals(uuid, command.userId.value)
        assertEquals("type1;type2", command.legacyNotificationTypes)
    }
}
