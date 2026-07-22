# Phase 4 Validation Architecture

## Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + MockK + KotlinX Coroutines Test |
| Config file | `app/build.gradle.kts` (already configured) |
| Quick run command | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest*"` |
| Full suite command | `./gradlew testDebugUnitTest` |

## Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SYNC-01 | SyncManager bọc network error bằng Result.failure (không crash) | unit | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest.testSyncNetworkFailure*"` | ❌ Wave 0 |
| SYNC-02 | SyncManager không đè FSRS state nếu local timestamp mới hơn server | unit | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest.testTimeBasedMerging*"` | ❌ Wave 0 |
| SYNC-02 | Không xóa review logs nếu pushSync thất bại | unit | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest.testReviewLogsPreservedOnFailure*"` | ❌ Wave 0 |

## Sampling Rate
- **Per task commit:** `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest*"`
- **Per wave merge:** `./gradlew testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd-verify-work`

## Wave 0 Gaps
- [ ] `data/src/test/java/com/nhimz/vocabmaster/data/sync/SyncManagerTest.kt` — covers SYNC-01, SYNC-02