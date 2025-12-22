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
VALUES ('12345678-90ab-cdef-1234-567890abcdef', 'type3;type4');

# 3. Try to send notification (should return 404)
curl -X POST -H "Content-Type: application/json" \
  localhost:8080/notify \
  -d '{"userId":"12345678-90ab-cdef-1234-567890abcdef","notificationType":"type3","message":"test"}'
# Expected: 404 Not Found
```

**Expected Logs:**
```
"Read strategy: NEW_ONLY for user 12345678-90ab-cdef-1234-567890abcdef"
"User not found: 12345678-90ab-cdef-1234-567890abcdef"
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
  -d '{"userId":"c3b2a1f0-e9d8-c7b6-a5f4-e3d2c1b0a9f8","notificationType":"type1","message":"test"}'
# Expected: 404 Not Found
```

**Expected Logs:**
```
"Read strategy: LEGACY_ONLY for user ... - no migration will be performed"
"User not found: c3b2a1f0-e9d8-c7b6-a5f4-e3d2c1b0a9f8"
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

---

## 🔌 Dual-Write Disabled (`migration.dual-write.enabled=false`)

### What Happens

When `migration.dual-write.enabled=false`, the `DualWriteUserRepositoryAdapter` bean is **not loaded**. Instead, Spring uses the simple `UserRepositoryAdapter` which:

- Reads **only from NEW table** (`user_subscriptions`)
- Writes **only to NEW table**
- **No dual-write** to legacy table
- **No fallback** to legacy table
- **No migration logic** is executed

### Configuration

```yaml
migration:
  dual-write:
    enabled: false  # ← Disables DualWriteUserRepositoryAdapter
```

### When to Use

Use this configuration **after migration is 100% complete** and you want to:
- Remove all migration overhead
- Simplify the codebase
- Prepare for removing legacy table

### Behavior Comparison

| Aspect | `dual-write.enabled=true` | `dual-write.enabled=false` |
|--------|---------------------------|----------------------------|
| **Bean Loaded** | `DualWriteUserRepositoryAdapter` | `UserRepositoryAdapter` |
| **Read from** | Strategy-based (NEW/LEGACY) | NEW only |
| **Write to** | Both NEW and LEGACY | NEW only |
| **Migration** | Depends on `read-source` | None |
| **Legacy Table** | Still used | Completely ignored |

### Test Steps

```bash
# 1. Edit application.yaml
migration:
  dual-write:
    enabled: false

# 2. Restart application

# 3. Register a user
curl -X POST -H "Content-Type: application/json" \
  localhost:8080/register \
  -d '{"id":"0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c","notifications":["type1","type5"]}'

# 4. Verify in database - user should ONLY be in NEW table
SELECT user_id FROM user_subscriptions WHERE user_id='0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c';
# Expected: User found

SELECT id FROM users_legacy WHERE id='t0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c';
# Expected: User NOT found (no dual-write)

# 5. Legacy-only users are NOT accessible
INSERT INTO users_legacy (id, notifications) VALUES ('550e8400-e29b-41d4-a716-446655440000', 'type1;type2');

curl -X POST -H "Content-Type: application/json" \
  localhost:8080/notify \
  -d '{"userId":"550e8400-e29b-41d4-a716-446655440000","notificationType":"type1","message":"test"}'
# Expected: 404 Not Found
```

### Expected Logs

```
# No DualWriteUserRepositoryAdapter logs
# Only UserRepositoryAdapter logs:
"Saving user 0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c with 2 subscriptions"
"Saved user 0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c successfully"
"Successfully registered user 0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c to 2 categories"
```

### ⚠️ Warning

Only disable dual-write when:
1. ✅ 100% of users have been migrated to NEW table
2. ✅ Verified all users are accessible via NEW table
3. ✅ Ready to deprecate legacy table

---

## ⏰ Batch Migration Job (`migration.batch-job.enabled=true`)

### What It Does

The batch migration job (`UserSubscriptionMigrationJob`) runs on a schedule to migrate legacy users to the new schema in batches. This is useful for:
- Migrating large user bases without impacting performance
- Running during off-peak hours (default: 2 AM daily)
- Progressing toward 100% migration faster than on-the-fly only

### Configuration

```yaml
migration:
  dual-write:
    enabled: true
    read-source: NEW_WITH_FALLBACK
  batch-job:
    enabled: true          # ← Enable batch migration
    batch-size: 1000       # Users per batch
    cron: "0 * * * * ?"    # Run every minute (for testing); default is "0 0 2 * * ?" (2 AM daily)
```

### Cron Expression Examples

| Expression | Schedule |
|------------|----------|
| `0 0 2 * * ?` | Every day at 2:00 AM |
| `0 0 * * * ?` | Every hour |
| `0 */30 * * * ?` | Every 30 minutes |
| `0 0 2 * * SAT,SUN` | Weekends at 2:00 AM |

### How It Works

1. **Finds unmigrated users**: Queries legacy table, filters those not yet in NEW table
2. **Processes in batches**: Migrates `batch-size` users at a time
3. **Logs progress**: Shows migrated count, failed count, and percentage
4. **Skips already migrated**: Doesn't re-migrate users

### Test Steps (Manual Trigger)

Since the cron job runs at scheduled times, for testing you can:

**Option 1: Change cron to run every minute**

```yaml
migration:
  batch-job:
    enabled: true
    batch-size: 100
    cron: "0 * * * * ?"  # Every minute (for testing)
```

```bash
# 1. Restart application with above config

# 2. Create several legacy-only users
psql -h localhost -U postgres -d codechallenge_db <<EOF
INSERT INTO users_legacy (id, notifications) VALUES 
  ('0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c', 'type1;type2'),
  ('9a8b7c6d-5e4f-3a2b-1c0d-9f8e7d6c5b4a', 'type3;type4'),
  ('4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a', 'type1;type5');
EOF

# 3. run the following query to verify they are not yet migrated
SELECT l.id
FROM users_legacy l
LEFT JOIN user_subscriptions n ON l.id = n.user_id
WHERE n.user_id IS NULL;

# 4. Wait for the batch job to run (check logs)

# 5. Verify users were migrated
SELECT l.id
FROM users_legacy l
LEFT JOIN user_subscriptions n ON l.id = n.user_id
WHERE n.user_id IS NULL;

# Expected: All 3 users migrated (no results)
```

**Option 2: Use Actuator endpoint (if exposed)**

If you have Spring Actuator configured, you could expose a manual trigger endpoint.

### Expected Logs

```
=== Starting batch migration job ===
Processing batch 0: 1000 users
Found 500 users to migrate in this batch
✓ Migrated user 0f1e2d3c-4b5a-6f7e-8d9c-0b1a2f3e4d5c
✓ Migrated user 9a8b7c6d-5e4f-3a2b-1c0d-9f8e7d6c5b4a
✓ Migrated user 4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a
Batch 1 completed: 500 migrated, 0 failed so far
=== Batch migration job completed ===
Total migrated: 500
Total failed: 0
Duration: 2 minutes 15 seconds
=== Migration Progress ===
Total legacy users: 10000
Total migrated: 5500
Progress: 55%
```

### Monitoring Progress

The batch job logs migration progress after each run:

```
=== Migration Progress ===
Total legacy users: 10000
Total migrated: 9500
Progress: 95%
```

When complete:
```
🎉 Migration is 100% complete!
```

### Recommended Migration Strategy

| Phase | Configuration | Description |
|-------|---------------|-------------|
| **1** | `read-source: NEW_WITH_FALLBACK` + `batch-job: false` | On-the-fly migration only |
| **2** | `read-source: NEW_WITH_FALLBACK` + `batch-job: true` | Add nightly batch migration |
| **3** | Monitor logs until `Progress: 100%` | Wait for completion |
| **4** | `read-source: NEW_ONLY` + `batch-job: false` | Disable batch, test NEW only |
| **5** | `dual-write.enabled: false` | Remove dual-write entirely |

### ⚠️ Important Notes

1. **Batch job requires `dual-write.enabled=true`**: The job uses `MigrateUserSubscriptionsUseCase` which writes to the new table.

2. **Don't run during peak hours**: Default cron is 2 AM to avoid performance impact.

3. **Monitor for failures**: Check logs for `✗ Failed to migrate user` messages.

4. **Batch size tuning**: Start with `1000`, reduce if database performance is affected.

