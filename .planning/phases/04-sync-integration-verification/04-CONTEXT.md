# Phase 4: Sync & Integration Verification - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 4 tập trung vào việc xác minh độ tin cậy của luồng đồng bộ hóa dữ liệu (SyncManager) và tính đúng đắn của hợp đồng API (API contract correctness). Cụ thể:
1. **SyncManager Error Handling (SYNC-01):** Đảm bảo luồng đồng bộ dữ liệu hoạt động ổn định, xử lý các lỗi HTTP hoặc lỗi mạng một cách mượt mà và có cơ chế thử lại (retry mechanism).
2. **Data Integrity & FSRS State Preservation (SYNC-02):** Đảm bảo quá trình đồng bộ dữ liệu hai chiều không làm hỏng hoặc hạ cấp trạng thái FSRS của thẻ từ vựng. Cần có chiến lược giải quyết xung đột rõ ràng.

**Nằm ngoài phạm vi của Phase này:**
- Các tính năng liên quan đến giao diện (đã xử lý xong ở Phase 3).
- Cấu trúc kiến trúc nội bộ hoặc thay đổi Database schemas.
- Viết lại toàn bộ backend (chỉ tinh chỉnh payload/endpoint nếu contract bị sai).
</domain>

<decisions>
## Implementation Decisions

### 1. Cơ chế Retry & Xử lý lỗi (SYNC-01)
- **D-01: Explicit/Manual Retry UI.** Lỗi mạng trong quá trình đồng bộ (Sync) sẽ không được retry ngầm tự động một cách quá mức. Thay vào đó, sau một vài nỗ lực nhẹ (ví dụ: OkHttp retry), hệ thống sẽ đẩy lỗi lên UI (SettingsScreen) để người dùng có thể thấy trạng thái "Sync Failed" và chủ động nhấn nút "Thử lại".
- **D-02: Graceful Degradation.** Khi Sync thất bại, ứng dụng vẫn phải hoạt động bình thường ở chế độ offline. Trạng thái FSRS offline được giữ nguyên.

### 2. Chiến lược giải quyết xung đột dữ liệu FSRS (SYNC-02)
- **D-03: Server-wins with Time-Based Merging (Trạng thái muộn nhất sẽ thắng).** Khi đồng bộ hai chiều `pullSync` và `pushSync`, việc quyết định ghi đè sẽ dựa trên mốc thời gian cập nhật. Mặc định ưu tiên trạng thái nào có dấu thời gian (timestamp) hoặc lịch sử review mới hơn để tránh tình trạng "hạ cấp" thẻ nhớ.
- **D-04: Bảo toàn Review Logs.** Tuyệt đối không xóa local review logs chưa được đồng bộ nếu quá trình `pushSync` thất bại. 

### 3. Phản hồi giao diện đồng bộ (UI Feedback)
- **D-05: Minimal Global Indication.** Trạng thái đồng bộ sẽ được quản lý ở SettingsScreen, nhưng có thể có một chỉ báo nhỏ (như biểu tượng mây) ở HomeScreen nếu đang tiến hành hoặc báo lỗi để người dùng dễ nhận biết (tuỳ biến nếu cần, ưu tiên Snackbars).

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
</canonical_refs>

<code_context>
## Existing Code Insights

### Established Patterns
- **SyncManager:** Đang nằm ở `:data` module, làm việc trực tiếp với `VocabDao` và `ApiClient`.
- **API Response Handling:** Retrofit interface đang trả về `Response<T>`.

### Integration Points
- Backend được giả định ở `http://127.0.0.1:8000/`. Hợp đồng payload (`SyncPayload`) phải match với những gì backend mong đợi (hoặc tự điều chỉnh app nếu phát hiện bất đồng).
</code_context>

<specifics>
## Specific Ideas
- Xem xét dùng `runCatching` để bao bọc các lời gọi Retrofit trong `SyncManager.sync()` để ngăn exception lọt ra ngoài, sau đó wrap vào các sealed classes như `Result` hoặc `NetworkResponse` để viewModel xử lý.
</specifics>

<deferred>
## Deferred Ideas
- Push notifications hoặc background sync định kỳ bằng WorkManager (Sẽ chuyển xuống milestone 2 để tránh scope creep hiện tại).
</deferred>

---

*Phase: 04-sync-integration-verification*
*Context gathered: 2026-07-22*
