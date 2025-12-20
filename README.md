# Notification System - Hexagonal Architecture

All the information about the code challenge is in [CODE_CHALLENGE.md](./CODE_CHALLENGE.md)

## ⭐ Implementation Details
For a complete overview of the solution architecture, design decisions, and recent improvements, please read **[IMPLEMENTATION_CHANGES.md](./IMPLEMENTATION_CHANGES.md)**

## 🚀 Quick Start

### Prerequisites
- Java 21
- Docker & Docker Compose
- Git

### Setup & Run

1. **Start infrastructure** (PostgreSQL):
   ```bash
   docker-compose up -d
   ```

2. **Build the project** (Git hooks configured automatically):
   ```bash
   ./gradlew build
   ```
   > First build automatically configures Git hooks - no manual setup needed!

3. **Start the application**:
   ```bash
   ./gradlew bootRun
   ```

4. **Test the API**:
   ```bash
   # Register user with notifications from multiple categories
   curl -X POST -H "Content-Type: application/json" localhost:8080/register \
     -d '{ "id": "bcce103d-fc52-4a88-90d3-9578e9721b36", "notifications": ["type1","type5"]}'

   # Send notification
   curl -X POST -H "Content-Type: application/json" localhost:8080/notify \
     -d '{ "userId": "bcce103d-fc52-4a88-90d3-9578e9721b36", "notificationType": "type5", "message": "your app rocks!"}'
   ```

## 📚 API Documentation (Swagger/OpenAPI)

Once the application is running, access the interactive documentation:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

**Features**:
- Interactive API testing
- Request/response schemas with examples
- Detailed endpoint documentation

## 🛠️ Development Tools

### Git Hooks (Automatic Setup)

This project uses Git hooks to enforce code quality standards.

**Automatic Configuration**:
```bash
# First build automatically configures hooks
./gradlew build
# or manually
./gradlew installGitHooks
```

**Pre-commit Validations**:
- ✅ Branch naming convention (`feat|fix|chore/NOJIRA/description`)
- ✅ Code formatting (Spotless + ktlint)
- ✅ Build success
- ✅ Unit tests pass
- ✅ Coverage thresholds met (80%+)
- ✅ Integration tests pass

**Note**: Hooks are configured in `.githooks/` and managed by Gradle automatically. No manual setup needed!

### Code Formatting (Spotless + ktlint)

Consistent code formatting is enforced using [Spotless](https://github.com/diffplug/spotless) with [ktlint](https://ktlint.github.io/).

```bash
# Check formatting (runs automatically on build)
./gradlew spotlessCheck

# Auto-fix formatting issues
./gradlew spotlessApply
```

**Configuration**:
- Indent: 4 spaces
- Max line length: 150 characters
- Kotlin official style guide

**Note**: Build will fail if code is not properly formatted.

### Code Coverage (JaCoCo)

Code coverage is measured using [JaCoCo](https://www.jacoco.org/jacoco/).

```bash
# Run tests with coverage report
./gradlew test jacocoTestReport

# Verify coverage thresholds
./gradlew jacocoTestCoverageVerification

# Run all tests with integration tests
./gradlew test integrationTest jacocoTestCoverageVerification
```

**Coverage Requirements**:
- **Overall**: 60% instruction coverage minimum
- **Domain Model & Application Layer**: 80% line coverage minimum

**Reports**:
- HTML Report: `build/reports/jacoco/test/html/index.html`
- XML Report: `build/reports/jacoco/test/jacocoTestReport.xml`

## 🏗️ Project Structure

```
src/
├── main/kotlin/de/dkb/api/codeChallenge/
│   ├── domain/                 # Business logic & entities
│   ├── application/            # Use cases & DTOs
│   └── infrastructure/         # Adapters, persistence, config
├── test/kotlin/               # Unit tests
└── integrationTest/kotlin/    # Integration tests
```

**Architecture**: Hexagonal (Ports & Adapters)
- **Domain**: Core business logic, pure and framework-agnostic
- **Application**: Use cases orchestrating domain logic
- **Infrastructure**: Adapters for REST, Kafka, database, etc.

## 📊 Key Features

✅ **Hexagonal Architecture** - Clean separation of concerns  
✅ **Dual-Write Migration** - Zero-downtime schema transition  
✅ **Category-Based Subscriptions** - Dynamic type resolution  
✅ **Backward Compatible API** - Existing clients work unchanged  
✅ **Comprehensive Testing** - Unit + Integration tests with high coverage  
✅ **Code Quality Automation** - Pre-commit hooks enforce standards  
✅ **API Documentation** - Swagger/OpenAPI for interactive testing  

## 🔄 Migration Strategy

This project implements a **Dual-Write Migration Pattern** to safely transition from type-based to category-based subscriptions with zero downtime.

### Read Source Strategies

The system supports three read strategies, configurable via `application.yaml`:

```yaml
migration:
  dual-write:
    enabled: true
    read-source: NEW_WITH_FALLBACK  # Options: NEW_WITH_FALLBACK, NEW_ONLY, LEGACY_ONLY
```

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `NEW_WITH_FALLBACK` | Read from new table first, fallback to legacy with on-the-fly migration | Default during migration |
| `NEW_ONLY` | Read only from new table, no fallback | After 100% migration complete |
| `LEGACY_ONLY` | Read only from legacy table, **NO migration** | Emergency rollback |

### Migration Phases

| Phase | Configuration | Description |
|-------|--------------|-------------|
| **1. Active Migration** | `read-source: NEW_WITH_FALLBACK` | Dual-write + lazy migration |
| **2. Batch Migration** | `batch-job.enabled: true` | Nightly batch processing |
| **3. Verification** | `read-source: NEW_ONLY` | Test with new table only |
| **4. Complete** | Remove dual-write code | Legacy table can be dropped |

### Strategy Behavior Summary

```
NEW_WITH_FALLBACK (Default)
├── ✅ Reads from NEW first
├── ✅ Falls back to LEGACY if not found
├── ✅ Performs on-the-fly migration
└── ✅ Saves migrated user to NEW table

NEW_ONLY (Post-Migration)
├── ✅ Reads ONLY from NEW table
├── ❌ No fallback to LEGACY
├── ❌ No migration
└── ⚡ Best performance (single query)

LEGACY_ONLY (Emergency Rollback)
├── ✅ Reads ONLY from LEGACY table
├── ❌ No access to NEW table
├── ❌ NO migration happens
└── 🔒 Safe for emergencies
```

### Emergency Rollback

If issues are detected with the new implementation:

```yaml
# application.yaml
migration:
  dual-write:
    read-source: LEGACY_ONLY  # Immediate rollback
```

This mode:
- Reads **only** from the legacy table
- Performs **no migration** (data in NEW table is untouched)
- Returns 404 for users that only exist in NEW table
- Ensures system stability during incident resolution

### Testing Strategies

For detailed testing instructions without code changes, see:
- **[migration_configuration_flow.md](./migration_configuration_flow.md)** - Complete testing guide

## 📝 Documentation Files

- **[CODE_CHALLENGE.md](./CODE_CHALLENGE.md)** - Original problem statement
- **[IMPLEMENTATION_CHANGES.md](./IMPLEMENTATION_CHANGES.md)** - Complete architecture & implementation details (v1.2)
- **[migration_configuration_flow.md](./migration_configuration_flow.md)** - Migration strategy pattern details



