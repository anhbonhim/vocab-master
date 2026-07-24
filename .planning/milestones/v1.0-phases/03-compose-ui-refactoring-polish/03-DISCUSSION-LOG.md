# Phase 3: Compose UI Refactoring & Polish - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-22
**Phase:** 03-compose-ui-refactoring-polish
**Areas discussed:** Độ chi tiết của Component, Lưu trạng thái Quiz, Hiệu ứng phản hồi (Animation), Chủ đề Sáng/Tối (Dark/Light mode), Chiến lược Navigation Type-Safe, Thông báo lỗi, Null Safety

---

## Độ chi tiết của Component

| Option | Description | Selected |
|--------|-------------|----------|
| Tái sử dụng Component hiện có | Sử dụng các component đã có (tái sử dụng DuolingoProgressBar, DuolingoOptionCard) để giữ tính đồng bộ, chỉ cấu trúc lại HomeScreen thành Container/Content. | |
| Tạo mới từ đầu | Tạo mới hoàn toàn các thành phần UI từ đầu theo chuẩn 3D của Duolingo (mất nhiều thời gian hơn nhưng kiểm soát chi tiết hơn). | ✓ |
| Để AI quyết định | Để AI tự quyết định mức độ cấu trúc tùy thuộc vào độ phức tạp của từng file. | |

**User's choice:** Tạo mới từ đầu
**Notes:**

---

## Lưu trạng thái Quiz

| Option | Description | Selected |
|--------|-------------|----------|
| Lưu các trạng thái cơ bản | Lưu index câu hỏi, danh sách câu hỏi và trạng thái đã trả lời (đúng/sai) vào SavedStateHandle. | |
| Chỉ lưu Index | Chỉ lưu index câu hỏi hiện tại, nạp lại câu hỏi từ Room DB sau khi xoay màn hình. | |
| Để AI quyết định | Để AI tự quyết định những state nào cần được lưu để tối ưu hóa hiệu suất và bộ nhớ. | ✓ |

**User's choice:** Để AI quyết định
**Notes:** 

---

## Hiệu ứng phản hồi (Animation)

| Option | Description | Selected |
|--------|-------------|----------|
| Rung nhẹ (Sai) / Bật lên (Đúng) | Rung lắc 3 lần (-20f -> 20f -> -10f -> 10f) cho câu trả lời sai; Hiệu ứng bật (scale từ 0.8 -> 1.1 -> 1.0) cho câu trả lời đúng. | |
| Chỉ đổi màu | Chỉ đổi màu (đỏ cho sai, xanh lá cho đúng) không kèm theo animation nào cả. | |
| Để AI quyết định | Để AI tự thiết kế các animation mượt mà nhất dựa trên Compose Animatable. | ✓ |

**User's choice:** Để AI quyết định
**Notes:**

---

## Chủ đề Sáng/Tối (Dark/Light mode)

| Option | Description | Selected |
|--------|-------------|----------|
| Bảng màu chung (Chỉ tối nền) | Dùng một bảng màu chung cho cả sáng và tối (chỉ tối màu nền đi), giữ nguyên màu DuolingoGreen làm chủ đạo. | ✓ |
| Hai bảng màu tách biệt | Tạo 2 bảng màu riêng biệt: màu Pastel cho Light Mode và màu Neon nổi bật cho Dark Mode. | |
| Để AI quyết định | Để AI tự động map màu Duolingo vào MaterialTheme ColorScheme sao cho dễ nhìn nhất. | |

**User's choice:** Bảng màu chung (Chỉ tối nền)
**Notes:**

---

## Chiến lược Navigation Type-Safe

| Option | Description | Selected |
|--------|-------------|----------|
| Chuyển đổi toàn bộ cùng lúc | Sửa toàn bộ file liên quan (`VocabMasterApp`, `HomeScreen`, `SettingsScreen`...) sang Serialization route trong một plan duy nhất. | ✓ |
| Chuyển đổi từng phần (Incremental) | Chuyển đổi riêng lẻ từng màn hình (ví dụ: làm màn hình Quiz trước, màn hình khác để sau). | |
| Để AI quyết định | Để AI tự quyết định kế hoạch chuyển đổi an toàn nhất và ít gây xung đột nhất. | |

**User's choice:** Chuyển đổi toàn bộ cùng lúc
**Notes:**

---

## Thông báo lỗi

| Option | Description | Selected |
|--------|-------------|----------|
| Ưu tiên Snackbar | Dùng Snackbar cho tất cả các thông báo không quá nghiêm trọng, chỉ dùng Dialog khi cần người dùng xác nhận rõ ràng. | ✓ |
| Dùng Dialog cho mọi lỗi | Dùng Dialog (cửa sổ nổi) cho mọi lỗi để buộc người dùng phải đóng nó lại. | |
| Để AI quyết định | Để AI tự thiết kế theo phong cách UI/UX thân thiện nhất của Compose. | |

**User's choice:** Ưu tiên Snackbar
**Notes:**

---

## Null Safety

| Option | Description | Selected |
|--------|-------------|----------|
| Ghi Log + Hiện lỗi | Nếu ép kiểu bị lỗi, đẩy state về UiState.Error kèm message và ghi log qua LocalLogger (Không được fail im lặng). | ✓ |
| Ghi Log + Che lỗi | Chỉ ghi log qua LocalLogger và trả về fallback value tĩnh (ví dụ: Empty object), che giấu lỗi đi. | |
| Để AI quyết định | Để AI thiết lập quy tắc bắt lỗi và safe-casting tối ưu nhất cho Compose state. | |

**User's choice:** Ghi Log + Hiện lỗi
**Notes:**

---

## the agent's Discretion

- Lưu trạng thái Quiz
- Hiệu ứng phản hồi (Animation)

## Deferred Ideas

Không có
