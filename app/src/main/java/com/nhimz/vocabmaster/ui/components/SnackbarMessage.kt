package com.nhimz.vocabmaster.ui.components

import androidx.compose.material3.SnackbarDuration

/**
 * Một message có thể được hiển thị qua [androidx.compose.material3.SnackbarHostState].
 *
 * Đây là giá trị bất biến mà các Container có thể đẩy lên [DuoSnackbarHost] (host ở
 * `VocabMasterApp`) thông qua callback `onShowSnackbar`. Tách kiểu dữ liệu này khỏi
 * UI giúp cho việc test logic show-message dễ dàng hơn, đồng thời cho phép các
 * layer khác (ViewModel, UseCase) định nghĩa message mà không cần biết về Compose.
 *
 * Liên kết:
 *  - PLAN.md D-04: Log and display errors gracefully (do not silently swallow).
 *  - PLAN.md D-05: Prioritize Snackbar (non-blocking) cho các lỗi thường gặp.
 *
 * @property text Nội dung hiển thị trên snackbar.
 * @property actionLabel Nhãn của action button phía bên phải (optional). Khi `null`
 *   thì snackbar chỉ có nút dismiss.
 * @property duration Khoảng thời gian snackbar hiển thị (short / long / indefinite).
 * @property isError Marker cho biết đây có phải là thông báo lỗi hay không — UI có
 *   thể dùng cờ này để đổi màu nền nếu cần (mặc định dùng palette của
 *   `DuoSnackbarHost`).
 */
data class SnackbarMessage(
    val text: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val isError: Boolean = false,
) {
    /**
     * Optional callback invoked when the user taps the action label on the
     * snackbar. Kept on the message so the producer (ViewModel/UseCase) can
     * fully describe the intent (e.g. "Thử lại" → triggerSync()) without the
     * UI layer needing to know which callback to dispatch for which message.
     *
     * Moved out of the primary constructor so it is excluded from the
     * auto-generated [equals]/[hashCode] — lambda instances are compared
     * by reference identity and would break equality-based deduplication.
     */
    var action: (() -> Unit)? = null
}
