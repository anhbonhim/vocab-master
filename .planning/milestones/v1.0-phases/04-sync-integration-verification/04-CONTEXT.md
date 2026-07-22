# Phase 04: Sync & Integration Verification - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 4 tập trung vào việc xác minh độ tin cậy của luồng đồng bộ hóa dữ liệu (SyncManager) và tính đúng đắn của hợp đồng API (API contract correctness). Cụ thể:
1. **SyncManager Error Handling (SYNC-01):** Đảm bảo luồng đồng bộ dữ liệu hoạt động ổn định, xử lý các lỗi HTTP hoặc mạng mượt mà, cung cấp Retry mechanism và Offline degradation.
2. **Data Integrity & FSRS State Preservation (SYNC-02):** Đảm bảo quá trình đồng bộ dữ liệu hai chiều không làm hỏng hoặc hạ cấp trạng thái FSRS của thẻ từ vựng. Cần có chiến lược giải quyết xung đột rõ ràng.

**Nằm ngoài phạm vi của Phase này:**
- Các tính năng liên quan đến giao diện người dùng (đã xử lý ở Phase 3).
- Thay đổi cấu trúc database cục bộ (đã xong ở Phase 1).
- Chỉnh sửa toàn bộ backend (chỉ tinh chỉnh payload/endpoint nếu contract bị sai).
</domain>

<decisions>
## Implementation Decisions

### Cơ chế Retry & Xử lý lỗi (SYNC-01)
- **D-01:** Explicit/Manual Retry UI. Không retry ngầm quá mức. Sau vài nỗ lực nhẹ (ví dụ: OkHttp retry), lỗi đẩy lên UI (SettingsScreen) để người dùng chủ động nhấn "Thử lại". — **Reversibility:** reversible — UI logic
- **D-02:** Graceful Degradation. Khi Sync thất bại, ứng dụng vẫn hoạt động bình thường ở chế độ offline. Trạng thái FSRS offline được giữ nguyên. — **Reversibility:** costly — Kiến trúc offline-first
- **D-06:** Cảnh báo Offline UI: Khi Sync thất bại liên tục, chỉ hiển thị cảnh báo offline mode nhỏ (snackbar/icon), app vẫn dùng bình thường. Hiện biểu tượng đám mây bị gạch chéo ở HomeScreen kèm theo Snackbar (chỉ hiện 1 lần). — **Reversibility:** reversible — Chỉ là thông báo UI
- **D-07:** Action trên Snackbar: Tích hợp nút "Thử lại" (Retry) trực tiếp lên Snackbar cảnh báo để người dùng thao tác nhanh. — **Reversibility:** reversible — Thay đổi action trên UI
- **D-08:** Offline Access: Cho phép người dùng tiếp tục thực hiện Quiz (Review thẻ) trong lúc đang Cảnh báo Offline. Dữ liệu review (logs) sẽ được lưu cục bộ và đồng bộ khi có mạng để bảo toàn FSRS. — **Reversibility:** costly — Thay đổi sâu logic luồng Review và Merge conflict khi có mạng lại

### Chiến lược giải quyết xung đột dữ liệu FSRS (SYNC-02)
- **D-03:** Server-wins with Time-Based Merging. Ưu tiên ghi đè dựa trên timestamp mới nhất. Trạng thái nào có lịch sử review/timestamp mới hơn sẽ thắng để tránh "hạ cấp" thẻ. — **Reversibility:** costly — Logic đồng bộ data quan trọng
- **D-04:** Bảo toàn Review Logs. Tuyệt đối không xóa local review logs chưa đồng bộ nếu `pushSync` thất bại. — **Reversibility:** costly — Logic DB và đồng bộ

### Phản hồi giao diện đồng bộ (UI Feedback)
- **D-05:** Minimal Global Indication. Quản lý trạng thái Sync chủ yếu ở SettingsScreen, nhưng có báo lỗi dạng Snackbar/biểu tượng mây ở HomeScreen (xem D-06, D-07) nếu đang Sync bị lỗi. — **Reversibility:** reversible — UI components

### the agent's Discretion
- Kiến trúc cụ thể cho việc lắng nghe và xử lý sự kiện Retry từ Snackbar trên HomeScreen đẩy về `SyncManager`.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Retrofit & Sync Architecture
- `https://developer.android.com/training/sync/network-requests` — Android Network connections & Retry strategies

### Project-Internal Files
- `.planning/REQUIREMENTS.md` — Defines SYNC-01, SYNC-02 requirements.
- `.planning/ROADMAP.md` — Lists Phase 4 goals and success criteria.
- `data/src/main/java/com/nhimz/vocabmaster/data/sync/SyncManager.kt` — Core synchronization logic to be audited.
- `data/src/main/java/com/nhimz/vocabmaster/data/remote/SyncPayload.kt` — DTO for sync.
- `data/src/main/java/com/nhimz/vocabmaster/data/remote/VocabularyApiService.kt` — API Retrofit interface defining sync endpoints.
- `app/src/main/java/com/nhimz/vocabmaster/ui/viewmodel/SettingsViewModel.kt` — Presentation layer consuming SyncManager.
- `.planning/codebase/ARCHITECTURE.md` — Clean Architecture rules, data flow.
- `.planning/codebase/CONCERNS.md` — Known bugs like exception swallowing in data layer.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`LocalLogger`** (`app/src/main/java/com/nhimz/vocabmaster/util/LocalLogger.kt`) — Dùng để log các lỗi exception khi sync thay vì swallow error.

### Established Patterns
- **SyncManager:** Đang nằm ở `:data` module, làm việc trực tiếp với `VocabDao` và `ApiClient`.
- **API Response Handling:** Retrofit interface đang trả về `Response<T>`.

### Integration Points
- Backend được giả định ở `http://127.0.0.1:8000/`. Hợp đồng payload (`SyncPayload`) phải match với những gì backend mong đợi (hoặc tự điều chỉnh app nếu phát hiện bất đồng).
</code_context>

<specifics>
## Specific Ideas
- Xem xét dùng `runCatching` để bao bọc các lời gọi Retrofit trong `SyncManager.sync()` để ngăn exception lọt ra ngoài, sau đó wrap vào các sealed classes như `Result` hoặc `NetworkResponse` để viewModel xử lý (đặc biệt xử lý các swallowed exceptions được nhắc đến trong CONCERNS.md).
</specifics>

<deferred>
## Deferred Ideas
- Push notifications hoặc background sync định kỳ bằng WorkManager (Sẽ chuyển xuống milestone 2 để tránh scope creep hiện tại).
</deferred>

---

*Phase: 04-sync-integration-verification*
*Context gathered: 2026-07-22*