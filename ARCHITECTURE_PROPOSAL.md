# Long-Term Architecture Proposal: Notification System Redesign

## Executive Summary

This document outlines a comprehensive architectural redesign of the notification system to address scalability, maintainability, and backward compatibility challenges. The proposal implements **Hexagonal Architecture** principles and introduces **dynamic configuration management** for notification categories and types.

## Current System Analysis

### Problems Identified

1. **Tight Coupling**: Business logic, persistence, and API layers are intertwined
2. **Type-Based Storage Model**: System stores individual notification types rather than category subscriptions
3. **Hardcoded Configuration**: Notification types and categories are defined in enums, requiring redeployment for changes
4. **Backward Compatibility Issues**: Legacy records with mixed categories (e.g., "type1;type5") need special handling
5. **Scale Problem**: When `type6` is added to Category A, existing users subscribed to `type1` won't receive `type6` notifications

### Current Data Model

```
users table:
- id: UUID
- notifications: VARCHAR (e.g., "type1;type2;type3" or "type1;type5")

Category A: type1, type2, type3 (+ type6 soon)
Category B: type4, type5
```

### Legacy Data Patterns

```sql
-- Single category subscription
"type1"           → Category A (early subscription)
"type1;type2;type3" → Category A (full subscription)
"type4;type5"     → Category B

-- Multi-category subscription (requires special handling)
"type1;type5"     → Category A + Category B
"type1;type2;type4;type5" → Category A + Category B
```

---

## Proposed Architecture: Hexagonal Architecture

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                     EXTERNAL ACTORS                         │
│  (REST Clients, Kafka Consumers, Admin Tools)              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   ADAPTERS (Driving Side)                   │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │ REST Controllers │  │ Kafka Listeners  │               │
│  │  - /register     │  │  - consume()     │               │
│  │  - /notify       │  └──────────────────┘               │
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Use Cases (Ports - Input)               │  │
│  │  - RegisterUserUseCase                               │  │
│  │  - SendNotificationUseCase                           │  │
│  │  - MigrateUserSubscriptionsUseCase                   │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                           │
│  ┌────────────────────────────────────────────────────┐    │
│  │           Domain Entities                          │    │
│  │  - User (Aggregate Root)                           │    │
│  │  - NotificationCategory                            │    │
│  │  - NotificationType                                │    │
│  │  - CategorySubscription                            │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │           Domain Services                          │    │
│  │  - CategoryResolutionService                       │    │
│  │  - SubscriptionValidator                           │    │
│  │  - LegacyDataMigrator                              │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │           Repository Interfaces (Output Ports)     │    │
│  │  - UserRepository                                  │    │
│  │  - CategoryConfigRepository                        │    │
│  │  - NotificationGateway                             │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                INFRASTRUCTURE LAYER                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            Adapters (Driven Side)                    │  │
│  │  ┌─────────────────┐  ┌───────────────────────────┐ │  │
│  │  │ JPA Repositories│  │ External Config Client    │ │  │
│  │  │  - UserJpa...   │  │  - Spring Cloud Config    │ │  │
│  │  └─────────────────┘  │  - or Config Service      │ │  │
│  │                       └───────────────────────────┘ │  │
│  │  ┌─────────────────┐  ┌───────────────────────────┐ │  │
│  │  │ Push Notification│  │ Database (PostgreSQL)     │ │  │
│  │  │  Service         │  │  - users_subscriptions    │ │  │
│  │  └─────────────────┘  │  - notification_categories│ │  │
│  │                       └───────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   EXTERNAL SYSTEMS                          │
│  (Config Server, Database, Push Services, Monitoring)      │
└─────────────────────────────────────────────────────────────┘
```

---

## Detailed Design

### 1. Domain Layer (Core Business Logic)

#### Domain Entities

```kotlin
// Domain Entity: Category-based subscription model
data class User(
    val id: UserId,
    val subscriptions: Set<CategorySubscription>
) {
    fun isSubscribedToCategory(category: NotificationCategory): Boolean {
        return subscriptions.any { it.category == category && it.active }
    }

    fun canReceiveNotification(type: NotificationType,
                              categoryResolver: CategoryResolutionService): Boolean {
        val category = categoryResolver.resolveCategory(type)
        return isSubscribedToCategory(category)
    }
}

data class CategorySubscription(
    val category: NotificationCategory,
    val subscribedAt: Instant,
    val active: Boolean = true
)

data class NotificationCategory(
    val id: CategoryId,
    val name: String,
    val types: Set<NotificationType>
)

data class NotificationType(
    val code: String,
    val category: CategoryId,
    val addedAt: Instant
)
```

#### Domain Services

```kotlin
// Resolves which category a notification type belongs to
interface CategoryResolutionService {
    fun resolveCategory(type: NotificationType): NotificationCategory
    fun resolveCategoriesFromLegacyTypes(types: Set<String>): Set<NotificationCategory>
}

// Validates subscription business rules
interface SubscriptionValidator {
    fun validate(userId: UserId, categories: Set<NotificationCategory>): ValidationResult
}

// Handles migration of legacy type-based data to category-based model
interface LegacyDataMigrator {
    fun migrateUserTypes(userId: UserId, legacyTypes: String): Set<CategorySubscription>
}
```

#### Repository Interfaces (Output Ports)

```kotlin
interface UserRepository {
    fun findById(id: UserId): User?
    fun save(user: User): User
    fun findByCategory(category: NotificationCategory): List<User>
}

interface CategoryConfigRepository {
    fun findAllCategories(): List<NotificationCategory>
    fun findCategoryById(id: CategoryId): NotificationCategory?
    fun findTypesByCategory(categoryId: CategoryId): Set<NotificationType>
}

interface NotificationGateway {
    fun sendNotification(userId: UserId, type: NotificationType, message: String)
}
```

---

### 2. Application Layer (Use Cases)

```kotlin
// Use Case: Register user with category-based subscriptions
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val categoryResolver: CategoryResolutionService,
    private val validator: SubscriptionValidator
) {
    fun execute(command: RegisterUserCommand): User {
        // Resolve categories from incoming types (backward compatible)
        val categories = categoryResolver.resolveCategoriesFromLegacyTypes(command.types)

        // Validate business rules
        validator.validate(command.userId, categories)

        // Create user with category subscriptions
        val user = User(
            id = command.userId,
            subscriptions = categories.map {
                CategorySubscription(
                    category = it,
                    subscribedAt = Instant.now()
                )
            }.toSet()
        )

        return userRepository.save(user)
    }
}

// Use Case: Send notification (with dynamic type resolution)
class SendNotificationUseCase(
    private val userRepository: UserRepository,
    private val categoryResolver: CategoryResolutionService,
    private val notificationGateway: NotificationGateway
) {
    fun execute(command: SendNotificationCommand) {
        val user = userRepository.findById(command.userId) ?: return

        val type = NotificationType(command.notificationType)

        // Dynamic resolution: check if user's subscribed categories include this type
        if (user.canReceiveNotification(type, categoryResolver)) {
            notificationGateway.sendNotification(
                userId = user.id,
                type = type,
                message = command.message
            )
        }
    }
}

// Use Case: Migrate existing users (one-time or batch)
class MigrateUserSubscriptionsUseCase(
    private val userRepository: UserRepository,
    private val legacyMigrator: LegacyDataMigrator
) {
    fun execute(userId: UserId, legacyData: String) {
        val subscriptions = legacyMigrator.migrateUserTypes(userId, legacyData)
        val user = User(id = userId, subscriptions = subscriptions)
        userRepository.save(user)
    }
}
```

---

### 3. Infrastructure Layer

#### Database Schema

```sql
-- New category-based schema
CREATE TABLE notification_categories (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE notification_types (
    code VARCHAR(50) PRIMARY KEY,
    category_id VARCHAR(50) NOT NULL REFERENCES notification_categories(id),
    added_at TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT true
);

CREATE TABLE user_subscriptions (
    user_id UUID NOT NULL,
    category_id VARCHAR(50) NOT NULL REFERENCES notification_categories(id),
    subscribed_at TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT true,
    PRIMARY KEY (user_id, category_id)
);

-- Legacy table (kept for backward compatibility during migration)
CREATE TABLE users_legacy (
    id UUID PRIMARY KEY,
    notifications VARCHAR(255) NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_user_subscriptions_category ON user_subscriptions(category_id);
CREATE INDEX idx_notification_types_category ON notification_types(category_id);
```

#### JPA Adapters

```kotlin
@Entity
@Table(name = "user_subscriptions")
data class UserSubscriptionEntity(
    @EmbeddedId
    val id: UserSubscriptionId,

    @Column(name = "subscribed_at")
    val subscribedAt: Instant,

    @Column(name = "active")
    val active: Boolean = true
)

@Embeddable
data class UserSubscriptionId(
    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "category_id")
    val categoryId: String
) : Serializable

// Adapter implementation
@Repository
class UserRepositoryAdapter(
    private val jpaRepository: UserSubscriptionJpaRepository,
    private val categoryRepository: CategoryConfigRepository
) : UserRepository {

    override fun findById(id: UserId): User? {
        val subscriptions = jpaRepository.findByIdUserId(id.value)
        return if (subscriptions.isNotEmpty()) {
            User(
                id = id,
                subscriptions = subscriptions.map { it.toDomain(categoryRepository) }.toSet()
            )
        } else null
    }

    override fun save(user: User): User {
        val entities = user.subscriptions.map { it.toEntity(user.id) }
        jpaRepository.saveAll(entities)
        return user
    }
}
```

---

### 4. Configuration Management

#### Option A: Spring Cloud Config (Recommended)

```yaml
# config-server/notification-config.yml
notification:
  categories:
    - id: "CATEGORY_A"
      name: "Category A"
      types:
        - code: "type1"
          addedAt: "2023-01-01T00:00:00Z"
        - code: "type2"
          addedAt: "2023-06-01T00:00:00Z"
        - code: "type3"
          addedAt: "2024-01-01T00:00:00Z"
        - code: "type6"
          addedAt: "2025-06-01T00:00:00Z"  # New type added

    - id: "CATEGORY_B"
      name: "Category B"
      types:
        - code: "type4"
          addedAt: "2023-01-01T00:00:00Z"
        - code: "type5"
          addedAt: "2023-06-01T00:00:00Z"
```

```kotlin
@Configuration
@ConfigurationProperties(prefix = "notification")
class NotificationConfigProperties {
    var categories: List<CategoryConfig> = emptyList()
}

data class CategoryConfig(
    val id: String,
    val name: String,
    val types: List<TypeConfig>
)

data class TypeConfig(
    val code: String,
    val addedAt: Instant
)

// Dynamic refresh support
@Component
@RefreshScope
class DynamicCategoryResolutionService(
    private val configProperties: NotificationConfigProperties
) : CategoryResolutionService {

    override fun resolveCategory(type: NotificationType): NotificationCategory {
        val category = configProperties.categories.find { category ->
            category.types.any { it.code == type.code }
        } ?: throw CategoryNotFoundException()

        return category.toDomain()
    }
}
```

#### Option B: Database-Driven Configuration

```kotlin
@Service
class DatabaseCategoryConfigRepository(
    private val categoryJpaRepository: NotificationCategoryJpaRepository,
    private val typeJpaRepository: NotificationTypeJpaRepository
) : CategoryConfigRepository {

    private val cache = ConcurrentHashMap<CategoryId, NotificationCategory>()

    @Scheduled(fixedRate = 60000) // Refresh every minute
    fun refreshCache() {
        val categories = categoryJpaRepository.findAll()
        categories.forEach { category ->
            val types = typeJpaRepository.findByCategoryId(category.id)
            cache[CategoryId(category.id)] = category.toDomain(types)
        }
    }

    override fun findAllCategories(): List<NotificationCategory> {
        return cache.values.toList()
    }
}
```

---

## Migration Strategy

### Phase 1: Dual-Write Pattern (Week 1-2)

- Keep existing `users` table with type-based storage
- Introduce new `user_subscriptions` table with category-based model
- Write to both tables during transition period
- Read from legacy table, fallback to new table

```kotlin
@Service
class TransitionalUserRepository(
    private val legacyRepository: LegacyUserRepository,
    private val newRepository: UserSubscriptionRepository,
    private val migrator: LegacyDataMigrator
) {
    fun save(user: User): User {
        // Dual write
        newRepository.save(user)
        legacyRepository.save(user.toLegacyFormat())
        return user
    }

    fun findById(id: UserId): User? {
        // Try new format first
        return newRepository.findById(id) ?: run {
            // Fallback to legacy, migrate on-the-fly
            legacyRepository.findById(id)?.let { legacyUser ->
                val migratedUser = migrator.migrate(legacyUser)
                newRepository.save(migratedUser) // Lazy migration
                migratedUser
            }
        }
    }
}
```

### Phase 2: Batch Migration (Week 2-3)

```kotlin
@Component
class UserSubscriptionMigrationJob(
    private val legacyRepository: LegacyUserRepository,
    private val migrator: LegacyDataMigrator,
    private val newRepository: UserSubscriptionRepository
) {

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    fun migrateBatch() {
        val batchSize = 1000
        var offset = 0

        do {
            val legacyUsers = legacyRepository.findAll(PageRequest.of(offset, batchSize))

            legacyUsers.forEach { legacyUser ->
                try {
                    val subscriptions = migrator.migrateUserTypes(
                        userId = legacyUser.id,
                        legacyTypes = legacyUser.notifications
                    )
                    val user = User(id = legacyUser.id, subscriptions = subscriptions)
                    newRepository.save(user)
                } catch (e: Exception) {
                    logger.error("Migration failed for user ${legacyUser.id}", e)
                }
            }

            offset++
        } while (legacyUsers.hasContent())
    }
}
```

### Phase 3: Cutover (Week 4)

- Verify all users migrated
- Switch reads to new table only
- Mark legacy table as deprecated
- Remove dual-write logic

---

## Handling Legacy Multi-Category Subscriptions

```kotlin
@Service
class LegacyDataMigrationService(
    private val categoryRepository: CategoryConfigRepository
) : LegacyDataMigrator {

    override fun migrateUserTypes(userId: UserId, legacyTypes: String): Set<CategorySubscription> {
        // Parse legacy format: "type1;type5" or "type1;type2;type3"
        val types = legacyTypes.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Group types by their category
        val categoriesMap = mutableMapOf<CategoryId, MutableSet<String>>()

        types.forEach { typeCode ->
            val category = findCategoryByType(typeCode)
            categoriesMap.getOrPut(category.id) { mutableSetOf() }.add(typeCode)
        }

        // Create category subscriptions
        return categoriesMap.keys.map { categoryId ->
            CategorySubscription(
                category = categoryRepository.findCategoryById(categoryId)!!,
                subscribedAt = Instant.now(),
                active = true
            )
        }.toSet()
    }

    private fun findCategoryByType(typeCode: String): NotificationCategory {
        return categoryRepository.findAllCategories()
            .first { category -> category.types.any { it.code == typeCode } }
    }
}
```

### Migration Examples

| Legacy Data             | Parsed Types                | Resolved Categories | Result Subscriptions |
|-------------------------|-----------------------------|--------------------|---------------------|
| `"type1"`               | `[type1]`                   | `[CATEGORY_A]`     | Category A          |
| `"type1;type2;type3"`   | `[type1, type2, type3]`     | `[CATEGORY_A]`     | Category A          |
| `"type4;type5"`         | `[type4, type5]`            | `[CATEGORY_B]`     | Category B          |
| `"type1;type5"`         | `[type1, type5]`            | `[CATEGORY_A, CATEGORY_B]` | Category A + B |
| `"type1;type2;type4"`   | `[type1, type2, type4]`     | `[CATEGORY_A, CATEGORY_B]` | Category A + B |

---

## API Backward Compatibility

### Maintaining Existing Endpoints

```kotlin
// Existing API - unchanged interface
@RestController
class NotificationController(
    private val registerUseCase: RegisterUserUseCase,
    private val sendNotificationUseCase: SendNotificationUseCase
) {

    // Backward compatible: still accepts types, converts to categories internally
    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegisterRequest): ResponseEntity<*> {
        val command = RegisterUserCommand(
            userId = UserId(request.id),
            types = request.notifications // ["type1", "type2", "type3"]
        )
        registerUseCase.execute(command)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/notify")
    fun sendNotification(@RequestBody request: NotifyRequest): ResponseEntity<*> {
        val command = SendNotificationCommand(
            userId = UserId(request.userId),
            notificationType = request.notificationType,
            message = request.message
        )
        sendNotificationUseCase.execute(command)
        return ResponseEntity.ok().build()
    }
}

// Optional: New category-based API (v2)
@RestController
@RequestMapping("/v2")
class NotificationControllerV2(
    private val registerUseCase: RegisterUserUseCaseV2
) {

    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegisterCategoryRequest): ResponseEntity<*> {
        val command = RegisterUserCategoryCommand(
            userId = UserId(request.userId),
            categories = request.categories.map { CategoryId(it) }
        )
        registerUseCase.execute(command)
        return ResponseEntity.ok().build()
    }
}
```

---

## Benefits of Proposed Solution

### 1. **Scalability**
- Adding `type6` to Category A requires only configuration change (no code deployment)
- All users subscribed to Category A automatically receive new types

### 2. **Maintainability**
- Clear separation of concerns via Hexagonal Architecture
- Domain logic isolated from infrastructure
- Easy to test with mock repositories

### 3. **Backward Compatibility**
- Existing API endpoints remain unchanged
- Legacy data migrated seamlessly
- Multi-category subscriptions preserved

### 4. **Flexibility**
- Configuration managed externally (Spring Cloud Config or database)
- Support for new categories without code changes
- Dynamic refresh of category definitions

### 5. **Performance**
- Category-based subscriptions reduce data redundancy
- Efficient querying via indexed category_id
- Caching strategies for frequently accessed config

---

## Testing Strategy

### Unit Tests (Domain Layer)

```kotlin
class CategoryResolutionServiceTest {
    @Test
    fun `should resolve type6 to Category A`() {
        val service = CategoryResolutionService()
        val type = NotificationType("type6")
        val category = service.resolveCategory(type)
        assertEquals("CATEGORY_A", category.id)
    }
}

class LegacyDataMigratorTest {
    @Test
    fun `should migrate multi-category subscription type1;type5`() {
        val migrator = LegacyDataMigrator()
        val result = migrator.migrateUserTypes(userId, "type1;type5")
        assertEquals(2, result.size)
        assertTrue(result.any { it.category.id == "CATEGORY_A" })
        assertTrue(result.any { it.category.id == "CATEGORY_B" })
    }
}
```

### Integration Tests

```kotlin
@SpringBootTest
class NotificationIntegrationTest {

    @Test
    fun `existing user subscribed to type1 should receive type6 notification`() {
        // Given: User subscribed to Category A (via type1)
        val userId = UUID.randomUUID()
        registerUser(userId, listOf("type1"))

        // When: type6 is added to Category A (via config update)
        addTypeToCategory("type6", "CATEGORY_A")

        // Then: User should receive type6 notification
        val result = sendNotification(userId, "type6", "New feature!")
        assertTrue(result.isSuccess)
    }
}
```

---

## Implementation Roadmap

### Sprint 1-2: Foundation
- [ ] Design and implement domain layer entities
- [ ] Create repository interfaces (ports)
- [ ] Implement use cases
- [ ] Set up Spring Cloud Config or database config
- [ ] Create new database schema

### Sprint 3: Migration Infrastructure
- [ ] Build legacy data migrator service
- [ ] Implement dual-write repository adapter
- [ ] Create migration job for batch processing
- [ ] Add monitoring and logging

### Sprint 4: Testing & Validation
- [ ] Comprehensive unit tests (domain layer)
- [ ] Integration tests with test containers
- [ ] Load testing with legacy data
- [ ] Validate backward compatibility

### Sprint 5: Deployment & Cutover
- [ ] Deploy to staging environment
- [ ] Run batch migration job
- [ ] Monitor migration progress
- [ ] Gradual rollout to production
- [ ] Remove dual-write logic after validation

### Sprint 6: Optimization
- [ ] Performance tuning
- [ ] Add caching layer
- [ ] Implement observability (metrics, traces)
- [ ] Documentation and runbooks

---

## Monitoring & Observability

```kotlin
@Component
class NotificationMetrics(
    private val meterRegistry: MeterRegistry
) {

    fun recordNotificationSent(category: String, type: String) {
        meterRegistry.counter("notifications.sent",
            "category", category,
            "type", type
        ).increment()
    }

    fun recordMigration(userId: UUID, categoriesCount: Int) {
        meterRegistry.counter("user.migration",
            "categories_count", categoriesCount.toString()
        ).increment()
    }
}
```

**Key Metrics to Track:**
- Notification delivery rate by category
- Migration progress (users migrated vs. total)
- API response times (old vs. new endpoints)
- Config refresh frequency and errors
- Category subscription distribution

---

## Security Considerations

1. **Input Validation**: Validate all incoming notification types against configured categories
2. **Authorization**: Ensure users can only modify their own subscriptions
3. **Config Security**: Protect Spring Cloud Config with authentication
4. **Audit Logging**: Track all subscription changes for compliance

---

## Conclusion

This architecture proposal provides a robust, scalable, and maintainable solution for the notification system. By adopting **Hexagonal Architecture** and **externalized configuration**, we achieve:

✅ **Zero-downtime** addition of new notification types
✅ **Backward compatibility** with legacy multi-category subscriptions
✅ **Clean architecture** with testable, decoupled components
✅ **Dynamic configuration** via Spring Cloud Config or database
✅ **Seamless migration** from type-based to category-based model

The migration strategy ensures a safe transition while maintaining full backward compatibility with existing clients.

---

## Appendix

### Technology Stack
- **Language**: Kotlin 2.2.0
- **Framework**: Spring Boot 3.5.3
- **Database**: PostgreSQL (with Liquibase migrations)
- **Config**: Spring Cloud Config or database-driven
- **Messaging**: Kafka (existing integration maintained)
- **Testing**: JUnit 5, Testcontainers

### References
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Spring Cloud Config Documentation](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)

---

**Document Version**: 1.0
**Last Updated**: 2025-12-15
**Author**: Architecture Team
**Status**: Proposed for Review
