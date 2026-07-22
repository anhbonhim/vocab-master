# Phase 4: Sync & Integration Verification - Research

**Researched:** 2026-07-22
**Domain:** Data Synchronization & Network Resilience
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01: Explicit/Manual Retry UI.** Lỗi mạng trong quá trình đồng bộ (Sync) sẽ không được retry ngầm tự động một cách quá mức. Thay vào đó, sau một vài nỗ lực nhẹ (ví dụ: OkHttp retry), hệ thống sẽ đẩy lỗi lên UI (SettingsScreen) để người dùng có thể thấy trạng thái "Sync Failed" và chủ động nhấn nút "Thử lại".
- **D-02: Graceful Degradation.** Khi Sync thất bại, ứng dụng vẫn phải hoạt động bình thường ở chế độ offline. Trạng thái FSRS offline được giữ nguyên.
- **D-03: Server-wins with Time-Based Merging (Trạng thái muộn nhất sẽ thắng).** Khi đồng bộ hai chiều `pullSync` và `pushSync`, việc quyết định ghi đè sẽ dựa trên mốc thời gian cập nhật. Mặc định ưu tiên trạng thái nào có dấu thời gian (timestamp) hoặc lịch sử review mới hơn để tránh tình trạng "hạ cấp" thẻ nhớ.
- **D-04: Bảo toàn Review Logs.** Tuyệt đối không xóa local review logs chưa được đồng bộ nếu quá trình `pushSync` thất bại.
- **D-05: Minimal Global Indication.** Trạng thái đồng bộ sẽ được quản lý ở SettingsScreen, nhưng có thể có một chỉ báo nhỏ (như biểu tượng mây) ở HomeScreen nếu đang tiến hành hoặc báo lỗi để người dùng dễ nhận biết (tuỳ biến nếu cần, ưu tiên Snackbars).

### the agent's Discretion
- Xem xét dùng `runCatching` để bao bọc các lời gọi Retrofit trong `SyncManager.sync()` để ngăn exception lọt ra ngoài, sau đó wrap vào các sealed classes như `Result` hoặc `NetworkResponse` để viewModel xử lý.

### Deferred Ideas (OUT OF SCOPE)
- Push notifications hoặc background sync định kỳ bằng WorkManager (Sẽ chuyển xuống milestone 2 để tránh scope creep hiện tại).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SYNC-01 | Verify data sync flow and ensure `SyncManager` handles backend request failures gracefully with retry mechanisms. | Error handling patterns with Retrofit & `runCatching`, mapping HTTP errors to UI state. |
| SYNC-02 | Ensure bidirectional data synchronization does not corrupt or downgrade FSRS card states. | Timestamp-based merge logic, idempotent database transactions, safeguarding review logs on push failure. |
</phase_requirements>

## Summary

Phase 4 tập trung vào việc gia cố lớp dữ liệu (Data Layer), cụ thể là `SyncManager`, để đảm bảo khả năng đồng bộ hóa hai chiều tin cậy và không làm mất/hạ cấp dữ liệu FSRS. Thay vì dựa vào cơ chế đồng bộ ngầm định (background sync), hệ thống sẽ xử lý lỗi mạng một cách minh bạch và đẩy trạng thái lên UI để người dùng quyết định (Explicit Retry). 

Đồng thời, cơ chế giải quyết xung đột được thiết kế theo nguyên tắc "Server-wins with Time-Based Merging", đảm bảo các thay đổi mới nhất (dựa trên timestamp) luôn được ưu tiên, và các bản ghi review nội bộ không bao giờ bị mất nếu đẩy lên server thất bại.

**Primary recommendation:** Sử dụng mô hình `runCatching` kết hợp với Kotlin `Result` (hoặc một domain-specific `SyncResult` sealed class) trong `SyncManager` để bao bọc mọi tương tác Retrofit, và áp dụng transaction cục bộ trong Room để bảo toàn tính nguyên vẹn của dữ liệu trong quá trình merge.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Hủy/Bọc lỗi API | API / Backend (`SyncManager`) | — | `SyncManager` chịu trách nhiệm catch các exception từ Retrofit và map chúng thành kết quả trả về an toàn. |
| Chiến lược Merge dữ liệu | Database / Storage (Room DAOs) | API / Backend | Việc quyết định ghi đè (timestamp check) cần thực hiện trong scope của Database Transaction để đảm bảo tính nguyên vẹn. |
| Hiển thị trạng thái Sync | Browser / Client (`SettingsScreen`) | — | UI/ViewModel nhận kết quả từ `SyncManager` và hiển thị UI (Success/Failed + nút Retry). |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Retrofit | 2.x | Network Requests | Tiêu chuẩn của Android để định nghĩa API contracts và thực hiện HTTP calls an toàn qua Coroutines. |
| Kotlin Coroutines | 1.7.x | Asynchronous execution | Cung cấp `runCatching` và `suspend` functions giúp quản lý luồng dữ liệu mượt mà, không block Main thread. |
| Room | 2.6.x | Local Database | Hỗ trợ `@Transaction` để đảm bảo thao tác merge/insert/update diễn ra nguyên vẹn. |

*(Note: Các thư viện này đã có sẵn trong dự án Android hiện tại).*

## Package Legitimacy Audit

> Phase này không yêu cầu cài đặt package mới nào từ bên ngoài, chỉ sử dụng các thư viện core hiện có (Retrofit, Room, Coroutines). Do đó không cần audit package mới.

## Architecture Patterns

### Recommended Project Structure
```
data/
├── src/main/java/com/nhimz/vocabmaster/data/
│   ├── sync/
│   │   ├── SyncManager.kt       # Xử lý logic đồng bộ chính, bọc runCatching
│   │   └── SyncResult.kt        # Sealed class định nghĩa kết quả (Success, Error)
│   ├── remote/
│   │   ├── SyncPayload.kt       # DTO khớp với hợp đồng API của backend
│   │   └── VocabularyApiService.kt # Định nghĩa endpoints cho Sync
```

### Pattern 1: Xử lý lỗi API với `runCatching` và `Result`
**What:** Bao bọc mọi thao tác I/O mạng (Retrofit) trong một khối try-catch an toàn hoặc dùng `runCatching` của Kotlin để chuyển Exception thành trạng thái (State) trả về cho ViewModel.
**When to use:** Khi thực hiện bất kỳ network call nào có rủi ro ném ra `IOException` (mất mạng) hoặc `HttpException` (lỗi server).
**Example:**
```kotlin
// [CITED: Android Official Network Architecture Guide]
suspend fun performSync(): Result<SyncPayload> {
    return runCatching {
        val response = apiService.pullSync()
        if (!response.isSuccessful) {
            throw HttpException(response) // Tự ném lỗi để runCatching bắt lại
        }
        response.body() ?: throw IllegalStateException("Empty body")
    }
}
```

### Pattern 2: Cập nhật CSDL theo Transaction và Timestamp
**What:** Khi merge dữ liệu từ server về local, so sánh `updatedAt` hoặc dấu thời gian của FSRS state. Nếu server mới hơn thì đè, cũ hơn thì bỏ qua. Mọi thao tác chạy trong `@Transaction`.
**When to use:** Khi nhận dữ liệu `SyncPayload` từ `pullSync` và chuẩn bị lưu vào Room.

### Anti-Patterns to Avoid
- **[Anti-pattern]:** Bắt lỗi (catch) âm thầm và trả về null mà không log hoặc đẩy lỗi lên UI. *Hậu quả:* Người dùng tưởng đồng bộ thành công nhưng thực chất thất bại. *Thay vào đó:* Trả về `Result.failure` hoặc `SyncResult.Error` rõ ràng.
- **[Anti-pattern]:** Xóa toàn bộ database cục bộ trước khi lưu dữ liệu từ server. *Hậu quả:* Mất các thay đổi offline chưa được push. *Thay vào đó:* Upsert (Update hoặc Insert) theo từng bản ghi dựa trên timestamp.
- **[Anti-pattern]:** Gọi Retrofit calls trên Main Thread. *Thay vào đó:* Luôn gọi trong `Dispatchers.IO` hoặc sử dụng `suspend` functions (Retrofit tự động đẩy sang background thread).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Xử lý HTTP status codes | Regex parser thủ công | Retrofit `Response.isSuccessful` | Retrofit hỗ trợ sẵn việc parse và quản lý status codes chuẩn xác. |
| Thread context switching | Hand-rolled Handlers/Threads | Coroutines `withContext(Dispatchers.IO)` | An toàn, chống memory leak và dễ tích hợp với cấu trúc ViewModel hiện có. |
| Rollback dữ liệu khi lỗi | Manual rollback logic | Room `@Transaction` | Room đảm bảo tính ACID; nếu quá trình merge thất bại giữa chừng, CSDL sẽ tự động rollback. |

## Common Pitfalls

### Pitfall 1: Xóa Review Logs khi push thất bại
**What goes wrong:** Xóa các review logs ở local ngay sau khi gọi API push, nhưng network bị rớt khiến server chưa nhận được.
**Why it happens:** Không kiểm tra trạng thái trả về của API call trước khi thực hiện thao tác xóa ở CSDL local.
**How to avoid:** Chỉ đánh dấu review logs là "đã đồng bộ" (hoặc xóa) NẾU VÀ CHỈ NẾU `pushSync` trả về HTTP 200/2xx Success.

### Pitfall 2: Bị "hạ cấp" (downgrade) trạng thái FSRS
**What goes wrong:** Thẻ từ vựng đã được ôn tập offline (độ khó giảm, interval tăng), nhưng khi sync lại bị đè bởi dữ liệu cũ từ server.
**Why it happens:** Cơ chế ghi đè mù quáng (blind overwrite) mà không xét dấu thời gian.
**How to avoid:** Luôn so sánh trường `updatedAt` của thẻ local và thẻ từ server trước khi tiến hành Update trong Room DAO.

## Code Examples

Verified patterns from official sources:

### Xử lý kết quả Sync trong ViewModel
```kotlin
// [CITED: Android Architecture Guidelines]
viewModelScope.launch {
    _syncState.value = SyncState.Loading
    val result = syncManager.performSync()
    result.fold(
        onSuccess = { 
            _syncState.value = SyncState.Success(it)
        },
        onFailure = { error ->
            _syncState.value = SyncState.Error(error.message ?: "Sync Failed")
            // Đẩy lỗi ra UI để hiển thị Snackbar hoặc nút Retry
        }
    )
}
```

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Hợp đồng API (SyncPayload) hiện tại có chứa trường `updatedAt` (hoặc tương tự) cho từng thực thể. | Architecture Patterns | Nếu không có timestamp, logic "Server-wins with Time-Based Merging" (D-03) không thể hoạt động. |

## Open Questions (RESOLVED)

*(Không có open questions nào nghiêm trọng cản trở quá trình lập kế hoạch).*

## Environment Availability

> Step 2.6: SKIPPED (no external dependencies identified beyond standard Android toolchain).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + MockK + KotlinX Coroutines Test |
| Config file | `app/build.gradle.kts` (already configured) |
| Quick run command | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest*"` |
| Full suite command | `./gradlew testDebugUnitTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SYNC-01 | SyncManager bọc network error bằng Result.failure (không crash) | unit | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest.testSyncNetworkFailure*"` | ❌ Wave 0 |
| SYNC-02 | SyncManager không đè FSRS state nếu local timestamp mới hơn server | unit | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest.testTimeBasedMerging*"` | ❌ Wave 0 |
| SYNC-02 | Không xóa review logs nếu pushSync thất bại | unit | `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest.testReviewLogsPreservedOnFailure*"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :data:testDebugUnitTest --tests "*SyncManagerTest*"`
- **Per wave merge:** `./gradlew testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `data/src/test/java/com/nhimz/vocabmaster/data/sync/SyncManagerTest.kt` — covers SYNC-01, SYNC-02

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | (Không thuộc scope Phase này) |
| V3 Session Management | no | (Không thuộc scope Phase này) |
| V4 Access Control | no | (Không thuộc scope Phase này) |
| V5 Input Validation | yes | Retrofit Gson/Moshi converter (Safe JSON parsing) |
| V6 Cryptography | no | — |

### Known Threat Patterns for Android Data Sync

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mất tính toàn vẹn dữ liệu khi app crash/mất mạng giữa chừng | Tampering/Denial | Sử dụng Room `@Transaction` cho toàn bộ quá trình merge, bảo đảm All-or-Nothing. |

## Sources

### Primary (HIGH confidence)
- [CITED: Android Developers - Network connections] - Kiến trúc xử lý lỗi mạng.
- [CITED: Android Developers - Room Transactions] - Đảm bảo nguyên vẹn dữ liệu khi merge.

### Secondary (MEDIUM confidence)
- [CITED: .planning/REQUIREMENTS.md] - Yêu cầu nghiệp vụ.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Các thư viện chuẩn của Android đã được chứng minh.
- Architecture: HIGH - Mẫu thiết kế chuẩn Repository/Manager với Coroutines `Result`.
- Pitfalls: HIGH - Các rủi ro thường gặp trong đồng bộ dữ liệu ngoại tuyến (offline-first).

**Research date:** 2026-07-22
**Valid until:** 2026-08-22
