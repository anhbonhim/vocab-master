package com.nhimz.vocabmaster.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nhimz.vocabmaster.ui.theme.ErrorRed
import com.nhimz.vocabmaster.ui.theme.ErrorRedLight
import com.nhimz.vocabmaster.ui.theme.ErrorRedLightDark

/**
 * Snackbar host theo phong cách Duolingo — gom Material3 [SnackbarHost] vào một
 * Composable duy nhất để host ở [com.nhimz.vocabmaster.ui.VocabMasterApp].
 *
 * Đây là phần "view" của error pipeline (D-04/D-05). Container screen nhận
 * callback `onShowSnackbar` từ parent và gọi nó khi state chuyển sang
 * `UiState.Error`. `DuoSnackbarHost` chỉ lo phần render.
 *
 * Màu nền: dùng `inverseSurface` để tạo độ tương phản cao với mọi theme
 * (light / dark), và `primary` cho action label theo palette Material3.
 *
 * Sử dụng:
 * ```
 * val snackbarHostState = remember { SnackbarHostState() }
 * Scaffold(snackbarHost = { DuoSnackbarHost(snackbarHostState) }) { ... }
 * scope.launch { snackbarHostState.showSnackbar(SnackbarMessage("Lỗi!").text) }
 * ```
 */
@Composable
fun DuoSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(8.dp)
    ) { data: SnackbarData ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Convenience overload dành cho việc gọi `showSnackbar` từ callback `onShowSnackbar`
 * với một [SnackbarMessage] thay vì truyền từng argument rời rạc.
 *
 * Trả về [androidx.compose.material3.SnackbarResult.Dismissed] khi timeout / user
 * dismiss, hoặc [androidx.compose.material3.SnackbarResult.ActionPerformed] khi
 * user click action label.
 */
suspend fun SnackbarHostState.showSnackbar(
    message: SnackbarMessage
): androidx.compose.material3.SnackbarResult {
    val result = showSnackbar(
        message = message.text,
        actionLabel = message.actionLabel,
        withDismissAction = message.actionLabel == null,
        duration = message.duration
    )
    // When the user taps the action label the host returns ActionPerformed;
    // dispatch the producer-supplied callback so the right behaviour fires
    // (D-07: "Thử lại" on the sync-error snackbar re-triggers sync, etc.).
    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
        message.action?.invoke()
    }
    return result
}

/**
 * Bảng màu Error dùng cho snackbar (đã có sẵn trong `ui.theme.ErrorRed`).
 * Expose ở đây để các Container screen có thể import một nguồn duy nhất.
 */
object DuoSnackbarPalette {
    val ErrorContainerLight = ErrorRedLight
    val ErrorContainerDark = ErrorRedLightDark
    val ErrorAccent = ErrorRed
}
