# ✅ DualWriteUserRepositoryAdapter - Strategy Pattern Implementation

## 📋 Summary

The `DualWriteUserRepositoryAdapter` uses the **Strategy Pattern** to implement dynamic read-source switching at runtime without code changes. This follows hexagonal architecture principles by delegating read operations to pluggable strategies.

### Strategies Overview

| Strategy | Read Source | Migration | Use Case |
|----------|-------------|-----------|----------|
| `NEW_WITH_FALLBACK` | NEW first, fallback to LEGACY | On-the-fly migration | Default during migration |
| `NEW_ONLY` | NEW only | None | After 100% migration complete |
| `LEGACY_ONLY` | LEGACY only | **None** (safe rollback) | Emergency rollback |

---

## 🎯 Architecture Overview

### Configuration Flow

```
application.yaml
    ↓
MigrationProperties (@ConfigurationProperties)
    ↓
DualWriteConfig.readSource: ReadSource (enum)
    ↓
UserReadStrategyConfiguration (Spring Bean)
    ↓
Map<ReadSource, UserReadStrategy>
    ↓
DualWriteUserRepositoryAdapter
    ↓
strategy() → Selected Strategy
```

### File Structure

```
infrastructure/persistence/adapter/
├── DualWriteUserRepositoryAdapter.kt          (Adapter - delegates to strategies)
├── UserRepositoryAdapter.kt                    (Simple implementation for NEW table)
└── strategy/
    ├── UserReadStrategy.kt                     (Interface)
    ├── NewWithFallbackReadStrategy.kt          (Implementation 1)
    ├── NewOnlyReadStrategy.kt                  (Implementation 2)
    ├── LegacyOnlyReadStrategy.kt               (Implementation 3)
    └── UserReadStrategyConfiguration.kt        (Spring wiring)
```

---

## 🧪 Testing Strategies

### Prerequisites

> ℹ️ **Testing Approach:**
> - **Existing REST endpoints**: `/register` and `/notify`
> - **Direct SQL queries**: Verify data in both tables
> - **Configuration changes**: Modify `application.yaml` to switch strategies
> - **Log inspection**: Debug logs show strategy execution

### Tools Required

| Tool | Purpose |
|------|---------|
| **curl** | Call REST endpoints |
| **psql** | Query database directly |
| **application.yaml** | Switch strategies |
| **Application logs** | Verify strategy execution |

### Database Setup

```bash
# Connect to database
psql -h localhost -U postgres -d codechallenge_db

# Check NEW table
SELECT * FROM user_subscriptions ORDER BY user_id;

# Check LEGACY table
SELECT id, notifications FROM users_legacy ORDER BY id;
```

---

## 📝 Test Case 1: NEW_WITH_FALLBACK (Default)

**Configuration:**
```yaml
migration:
  dual-write:
    enabled: true
    read-source: NEW_WITH_FALLBACK
```

**Test Steps:**

```bash
# 1. Register a user (writes to both tables)
curl -X POST -H "Content-Type: application/json" \
  localhost:8080/register \
  -d '{"id":"a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d","notifications":["type1","type5"]}'

# 2. Verify dual-write in psql:
SELECT 'NEW' as source, user_id FROM user_subscriptions WHERE user_id='a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d'
UNION ALL
SELECT 'LEGACY', id FROM users_legacy WHERE id='a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d';

# 3. Create legacy-only user (simulating unmigrated data)
INSERT INTO users_legacy (id, notifications) 
VALUES ('99999999-9999-9999-9999-999999999999', 'type1;type2');

# 4. Send notification to legacy-only user (triggers migration)
curl -X POST -H "Content-Type: application/json" \
  localhost:8080/notify \
  -d '{"userId":"99999999-9999-9999-9999-999999999999","notificationType":"type1","message":"test"}'

# 5. Verify user was migrated to NEW table
SELECT user_id FROM user_subscriptions WHERE user_id='99999999-9999-9999-9999-999999999999';
```

**Expected Logs:**
```
"Performing on-the-fly migration for user 99999999-..."
"Migrating user 99999999-...: 'type1;type2'"
"Successfully migrated user 99999999-...: 2 types -> 1 categories"
"On-the-fly migration successful for user 99999999-..."
```

**✅ Key Observations:**
- Write operations create data in **both** tables
- Read operations check NEW **first**, fallback to LEGACY
- Legacy-only users are **automatically migrated** on first read
- Subsequent reads are **faster** (no fallback needed)

---

## 📝 Test Case 2: NEW_ONLY

**Configuration:**
```yaml
migration:
  dual-write:
    enabled: true
    read-source: NEW_ONLY
```

**Test Steps:**

```bash
# 1. Restart application with NEW_ONLY config

# 2. Create legacy-only user
INSERT INTO users_legacy (id, notifications) 
VALUES ('legacy-only-user-uuid', 'type3;type4');

# 3. Try to send notification (should return 404)
curl -X POST -H "Content-Type: application/json" \
  localhost:8080/notify \
  -d '{"userId":"legacy-only-user-uuid","notificationType":"type3","message":"test"}'
# Expected: 404 Not Found
```

**Expected Logs:**
```
"Read strategy: NEW_ONLY for user legacy-only-user-uuid"
"User not found: legacy-only-user-uuid"
```

**✅ Key Observations:**
- Write still goes to **both** tables (safety)
- Read **only queries NEW** table
- Legacy-only users return **404**
- **No migration** happens
- **Best performance** (single DB query)

---

## 📝 Test Case 3: LEGACY_ONLY (Emergency Rollback)

**Configuration:**
```yaml
migration:
  dual-write:
    enabled: true
    read-source: LEGACY_ONLY
```

> ⚠️ **IMPORTANT**: In `LEGACY_ONLY` mode:
> - **NO migration happens** - data is NOT written to the new table
> - **NO "Migrating user..." logs** should appear
> - Only reads from `users_legacy` table
> - Users that exist only in the NEW table will return 404

**Test Steps:**

```bash
# 1. Restart application with LEGACY_ONLY config

# 2. Record table counts BEFORE test
SELECT 'NEW' as source, COUNT(*) FROM user_subscriptions
UNION ALL
SELECT 'LEGACY', COUNT(*) FROM users_legacy;

# 3. Send notification to user in LEGACY
curl -X POST -H "Content-Type: application/json" \
  localhost:8080/notify \
  -d '{"userId":"99999999-9999-9999-9999-999999999999","notificationType":"type1","message":"test"}'

# 4. Verify counts are EXACTLY the same (no migration!)
SELECT 'NEW' as source, COUNT(*) FROM user_subscriptions
UNION ALL
SELECT 'LEGACY', COUNT(*) FROM users_legacy;

# 5. Try user that only exists in NEW (should return 404)
curl -X POST -H "Content-Type: application/json" \
  localhost:8080/notify \
  -d '{"userId":"new-only-user-uuid","notificationType":"type1","message":"test"}'
# Expected: 404 Not Found
```

**Expected Logs:**
```
"Read strategy: LEGACY_ONLY for user ... - no migration will be performed"
"Converting legacy user ... with types: '...' (no migration)"
"Legacy user ... converted to X categories (no migration performed)"
```

**❌ You should NOT see:**
```
"Migrating user ..."
"Successfully migrated user ..."
```

**✅ Key Observations:**
- Read **only from LEGACY** (isolated from NEW)
- **NO migration happens**
- Users in NEW are **not accessible**
- Write still dual-writes (for consistency when fixed)
- **Data in NEW is untouched** (safe for recovery)

---

## 🔀 Emergency Rollback Procedure

```bash
# 1. Detect issue in NEW table

# 2. Edit application.yaml:
#    Change: read-source: NEW_WITH_FALLBACK
#    To:     read-source: LEGACY_ONLY

# 3. Restart application (rolling deployment)

# 4. Investigate and fix NEW table problem

# 5. When ready, revert:
#    Change: read-source: LEGACY_ONLY
#    To:     read-source: NEW_WITH_FALLBACK

# 6. Restart application
```

**Zero downtime** (just need a rolling restart)

---

## 📊 Strategy Comparison

| Aspect | NEW_WITH_FALLBACK | NEW_ONLY | LEGACY_ONLY |
|--------|-------------------|----------|-------------|
| **Read from** | NEW → LEGACY | NEW only | LEGACY only |
| **Write to** | Both | Both | Both |
| **Migration** | On-the-fly | None | None |
| **DB Queries** | 1-2 | 1 | 1 |
| **Performance** | Normal | Best | Normal |
| **Use Case** | Active migration | Post-migration | Emergency |

---

## ✅ Why Strategy Pattern

- **No conditionals**: Clean adapter without massive `when` blocks
- **Encapsulation**: Each strategy handles its own logic
- **Extensible**: Add new strategy without modifying adapter
- **Testable**: Each strategy tested independently
- **Maintainable**: Clear responsibility separation
- **Flexible**: Switch strategies at runtime via config

---

## ✅ Verification Checklist

### NEW_WITH_FALLBACK
- [ ] Dual-write creates users in both tables
- [ ] Fallback finds legacy-only users
- [ ] On-the-fly migration happens automatically
- [ ] Logs show "Migrating user..." and "Successfully migrated..."

### NEW_ONLY
- [ ] Dual-write still active
- [ ] Legacy-only users return 404
- [ ] No migration messages in logs
- [ ] Single DB query (faster)

### LEGACY_ONLY
- [ ] Legacy users found and returned
- [ ] New-only users return 404
- [ ] **NO migration** (no "Migrating user..." logs)
- [ ] Table counts remain **unchanged**
- [ ] Logs show "no migration will be performed"

---

**Status**: ✅ Strategy Pattern fully implemented and tested

