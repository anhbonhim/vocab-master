package com.nhimz.vocabmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart

/**
 * State-only value object consumed by [SettingsScreenContent]. The Container
 * (`SettingsScreen`) builds this from the ViewModels and forwards it.
 */
data class SettingsUiModel(
    val dailyGoalXp: Int = 10,
    val theme: String = "SYSTEM",
    val desiredRetention: Double = 0.90,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = true,
    val isDebugBuild: Boolean = false
)

/**
 * Callbacks for every user-intent the Settings screen exposes. The Content
 * never calls into ViewModels — it just funnels the user action back up.
 */
data class SettingsActions(
    val onDailyGoalChange: (Int) -> Unit = {},
    val onRetentionChange: (Double) -> Unit = {},
    val onThemeChange: (String) -> Unit = {},
    val onBackup: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onReminderTimeChange: (hour: Int, minute: Int, enabled: Boolean) -> Unit = { _, _, _ -> },
    val onRequestResetProgress: () -> Unit = {},
    val onRequestDeleteAccount: () -> Unit = {},
    val onResetProgress: () -> Unit = {},
    val onDeleteAccount: () -> Unit = {},
    val onShowLicenses: () -> Unit = {},
    val onNavigateToDebugPanel: () -> Unit = {}
)

/**
 * Which destructive confirmation dialog is currently visible. The Container
 * holds a `var` of this type and passes the current value down so the
 * Content can render the matching dialog body.
 */
enum class DestructiveDialog {
    None,
    ResetProgress,
    DeleteAccount
}

/**
 * Stateless UI for the Settings screen (Plan 03-01, Task 3).
 *
 * The Content takes a [SettingsUiModel] + [SettingsActions] pair and a
 * [DestructiveDialog] state. It does not call into any ViewModel or trigger
 * navigation — those concerns are hoisted to the Container.
 *
 * Per 03-UI-SPEC.md Copywriting Contract the destructive dialog text is:
 *  - "Đặt lại tiến trình": "Bạn có chắc muốn đặt lại toàn bộ tiến trình học? Hành động này không thể hoàn tác."
 *  - "Xóa tài khoản": "Tài khoản và toàn bộ dữ liệu học tập sẽ bị xóa vĩnh viễn. Tiếp tục?"
 *
 * Both copy strings are kept as constants here so the dialog renders with
 * correct text without the Container having to construct them.
 */
@Composable
fun SettingsScreenContent(
    state: SettingsUiModel,
    actions: SettingsActions,
    destructiveDialog: DestructiveDialog,
    onDestructiveDialogDismiss: () -> Unit,
    onLicensesDismiss: () -> Unit,
    showLicensesDialog: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Cài đặt ứng dụng",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Offline-first notice
        SettingsCard(title = "Chế độ ngoại tuyến") {
            Text(
                text = "Vocab Master hoạt động hoàn toàn ngoại tuyến. Tiến độ của bạn được lưu trữ cục bộ trên thiết bị và không gửi lên bất kỳ server nào.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Goal
        SettingsCard(title = "Mục tiêu hàng ngày") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Thời gian học mong muốn", fontSize = 14.sp)
                    Text(
                        text = "${state.dailyGoalXp} Phút",
                        fontWeight = FontWeight.Bold,
                        color = GradientStart
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = state.dailyGoalXp.toFloat(),
                    onValueChange = { actions.onDailyGoalChange(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = GradientStart,
                        activeTrackColor = GradientStart,
                        inactiveTrackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FSRS Retention
        SettingsCard(title = "Thuật toán FSRS Spaced Repetition") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tỷ lệ ghi nhớ mục tiêu (Retention)", fontSize = 14.sp)
                    Text(
                        text = String.format(java.util.Locale.US, "%.2f", state.desiredRetention),
                        fontWeight = FontWeight.Bold,
                        color = GradientStart
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = state.desiredRetention.toFloat(),
                    onValueChange = { actions.onRetentionChange(it.toDouble()) },
                    valueRange = 0.8f..0.95f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = GradientStart,
                        activeTrackColor = GradientStart,
                        inactiveTrackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
                Text(
                    text = "Lưu ý: Tỷ lệ cao hơn (0.95) sẽ tăng tần suất ôn tập thẻ ghi nhớ để giữ trí nhớ lâu hơn. Mức tối ưu khuyến nghị là 0.90.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Reminder
        SettingsCard(title = "Nhắc nhở hàng ngày") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Nhận thông báo từ vựng", fontSize = 14.sp)
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = {
                            actions.onReminderTimeChange(state.reminderHour, state.reminderMinute, it)
                        }
                    )
                }

                if (state.reminderEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "\u25B2",
                                modifier = Modifier.clickable {
                                    val nextHour = (state.reminderHour + 1) % 24
                                    actions.onReminderTimeChange(
                                        nextHour, state.reminderMinute, state.reminderEnabled
                                    )
                                }.padding(8.dp),
                                fontSize = 18.sp,
                                color = GradientStart
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%02d", state.reminderHour),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\u25BC",
                                modifier = Modifier.clickable {
                                    val nextHour = if (state.reminderHour - 1 < 0) 23 else state.reminderHour - 1
                                    actions.onReminderTimeChange(
                                        nextHour, state.reminderMinute, state.reminderEnabled
                                    )
                                }.padding(8.dp),
                                fontSize = 18.sp,
                                color = GradientStart
                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "\u25B2",
                                modifier = Modifier.clickable {
                                    val nextMin = (state.reminderMinute + 5) % 60
                                    actions.onReminderTimeChange(
                                        state.reminderHour, nextMin, state.reminderEnabled
                                    )
                                }.padding(8.dp),
                                fontSize = 18.sp,
                                color = GradientStart
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%02d", state.reminderMinute),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\u25BC",
                                modifier = Modifier.clickable {
                                    val nextMin = if (state.reminderMinute - 5 < 0) 55 else state.reminderMinute - 5
                                    actions.onReminderTimeChange(
                                        state.reminderHour, nextMin, state.reminderEnabled
                                    )
                                }.padding(8.dp),
                                fontSize = 18.sp,
                                color = GradientStart
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme
        SettingsCard(title = "Hệ thống") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Giao diện", fontSize = 14.sp)
                    Row {
                        listOf("LIGHT", "DARK", "SYSTEM").forEach { t ->
                            val isSelected = t == state.theme
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.horizontalGradient(
                                                listOf(GradientStart, GradientEnd)
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(Color.Transparent, Color.Transparent)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { actions.onThemeChange(t) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (t) {
                                        "LIGHT" -> "Sáng"
                                        "DARK" -> "Tối"
                                        else -> "Hệ thống"
                                    },
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backup & Restore
        SettingsCard(title = "Sao lưu & Khôi phục") {
            Column {
                Text(
                    text = "Lưu tiến độ học tập, thẻ từ vựng và lịch sử ôn tập của bạn thành tệp JSON hoặc khôi phục lại từ một bản sao lưu trước đó.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = actions.onBackup,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GradientStart),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Sao lưu tiến độ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = actions.onRestore,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Khôi phục tiến độ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== Destructive Actions (Plan 03-01, Task 3) =====
        // Per 03-UI-SPEC.md Copywriting Contract, destructive actions require
        // a confirmation dialog (D-05). The dialog text wraps cleanly thanks
        // to the explicit TextOverflow and fontSize configuration; Material3's
        // AlertDialog text slot has a `Column` that natively wraps long text.
        SettingsCard(title = "Dữ liệu nguy hiểm") {
            Column {
                Text(
                    text = "Các hành động dưới đây không thể hoàn tác. Vui lòng cân nhắc trước khi xác nhận.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = actions.onRequestResetProgress,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Đặt lại tiến trình",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = actions.onRequestDeleteAccount,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Xóa tài khoản",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About
        SettingsCard(title = "Thông tin") {
            Button(
                onClick = actions.onShowLicenses,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Nguồn dữ liệu & Bản quyền",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (state.isDebugBuild) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCard(title = "Developer") {
                Button(
                    onClick = actions.onNavigateToDebugPanel,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "Mở Debug Panel",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    // ===== Destructive confirmation dialogs =====
    when (destructiveDialog) {
        DestructiveDialog.ResetProgress -> {
            DestructiveConfirmationDialog(
                title = "Đặt lại tiến trình",
                body = RESET_PROGRESS_BODY,
                confirmLabel = "Đặt lại",
                onConfirm = {
                    actions.onResetProgress()
                    onDestructiveDialogDismiss()
                },
                onDismiss = onDestructiveDialogDismiss
            )
        }
        DestructiveDialog.DeleteAccount -> {
            DestructiveConfirmationDialog(
                title = "Xóa tài khoản",
                body = DELETE_ACCOUNT_BODY,
                confirmLabel = "Xóa vĩnh viễn",
                onConfirm = {
                    actions.onDeleteAccount()
                    onDestructiveDialogDismiss()
                },
                onDismiss = onDestructiveDialogDismiss
            )
        }
        DestructiveDialog.None -> Unit
    }

    // ===== Licenses dialog (existing) =====
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = onLicensesDismiss,
            title = { Text("Nguồn dữ liệu & Bản quyền", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Vocab Master cam kết sử dụng dữ liệu học thuật hợp pháp. Ứng dụng tích hợp các nguồn dữ liệu mở dưới đây:\n\n" +
                                "1. CEFR-J Wordlist v1.6\n" +
                                "• Bản quyền: Yukio Tono Laboratory, TUFS.\n" +
                                "• Sử dụng miễn phí cho nghiên cứu và thương mại.\n\n" +
                                "2. Open English WordNet 2025\n" +
                                "• Bản quyền: Dự án OEWN (McCrae et al.).\n" +
                                "• Giấy phép: Creative Commons Attribution 4.0 (CC BY 4.0).\n\n" +
                                "3. Tatoeba Corpus (Text & Audio)\n" +
                                "• Bản quyền: Cộng đồng đóng góp Tatoeba.org.\n" +
                                "• Giấy phép: CC BY 2.0 FR (Text) và CC BY 4.0 / CC0 (Audio).\n\n" +
                                "4. Wiktionary Data\n" +
                                "• Bản quyền: Wikimedia Foundation.\n" +
                                "• Giấy phép: CC BY-SA 4.0.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onLicensesDismiss) {
                    Text("Đóng")
                }
            }
        )
    }
}

/**
 * Confirmation dialog for a destructive action. The body text is rendered
 * with explicit `softWrap = true` (default) and the dialog content lives in
 * a [Column] which Material3's `AlertDialog` already constrains to the
 * dialog's text slot — long copy wraps without clipping on small screens.
 */
@Composable
private fun DestructiveConfirmationDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            // The text slot is a Column; long body text wraps across multiple
            // lines without horizontal overflow. We use the default fontSize
            // to stay readable on small screens.
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/** Copywriting Contract copy for the "Đặt lại tiến trình" dialog. */
private const val RESET_PROGRESS_BODY =
    "Bạn có chắc muốn đặt lại toàn bộ tiến trình học? Hành động này không thể hoàn tác."

/** Copywriting Contract copy for the "Xóa tài khoản" dialog. */
private const val DELETE_ACCOUNT_BODY =
    "Tài khoản và toàn bộ dữ liệu học tập sẽ bị xóa vĩnh viễn. Tiếp tục?"

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
