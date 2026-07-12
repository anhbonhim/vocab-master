package com.nhimz.vocabmaster.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.nhimz.vocabmaster.notification.NotificationScheduler
import com.nhimz.vocabmaster.BuildConfig
import com.nhimz.vocabmaster.ui.theme.GradientEnd
import com.nhimz.vocabmaster.ui.theme.GradientStart
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    notificationScheduler: NotificationScheduler,
    onNavigateToTopicPicker: () -> Unit,
    onNavigateToDebugPanel: () -> Unit
) {
    val context = LocalContext.current

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            settingsViewModel.backupData(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Sao lưu dữ liệu thành công!", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, "Sao lưu thất bại: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            settingsViewModel.restoreData(
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, "Khôi phục dữ liệu thành công!", Toast.LENGTH_SHORT).show()
                    viewModel.checkOnboardingStatus()
                },
                onError = { error ->
                    Toast.makeText(context, "Khôi phục thất bại: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    val dailyGoal by viewModel.dailyGoalMinutes.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val language by viewModel.language.collectAsState()
    val desiredRetention by viewModel.desiredRetention.collectAsState()

    // SharedPreferences to save reminder details
    val sharedPrefs = remember { context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE) }
    var hour by remember { mutableIntStateOf(sharedPrefs.getInt("reminder_hour", 9)) }
    var minute by remember { mutableIntStateOf(sharedPrefs.getInt("reminder_minute", 0)) }
    var isReminderEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("reminder_enabled", true)) }

    fun updateReminderTime(h: Int, m: Int, enabled: Boolean) {
        hour = h
        minute = m
        isReminderEnabled = enabled

        sharedPrefs.edit()
            .putInt("reminder_hour", h)
            .putInt("reminder_minute", m)
            .putBoolean("reminder_enabled", enabled)
            .apply()

        if (enabled) {
            notificationScheduler.scheduleDailyNotification(h, m)
        } else {
            notificationScheduler.cancelNotification()
        }
    }

    Column(
        modifier = Modifier
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

        // 1. Daily Study Goal minutes slider
        SettingsCard(title = "Mục tiêu hàng ngày") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Thời gian học mong muốn", fontSize = 14.sp)
                    Text(text = "$dailyGoal Phút", fontWeight = FontWeight.Bold, color = GradientStart)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = dailyGoal.toFloat(),
                    onValueChange = { viewModel.setDailyGoal(it.toInt()) },
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

        // 2. FSRS Desired Retention slider
        SettingsCard(title = "Thuật toán FSRS Spaced Repetition") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tỷ lệ ghi nhớ mục tiêu (Retention)", fontSize = 14.sp)
                    Text(text = String.format(java.util.Locale.US, "%.2f", desiredRetention), fontWeight = FontWeight.Bold, color = GradientStart)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = desiredRetention.toFloat(),
                    onValueChange = { viewModel.setDesiredRetention(it.toDouble()) },
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

        // 3. Daily reminder time picker
        SettingsCard(title = "Nhắc nhở hàng ngày") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Nhận thông báo từ vựng", fontSize = 14.sp)
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { updateReminderTime(hour, minute, it) }
                    )
                }

                if (isReminderEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour controls
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "▲",
                                modifier = Modifier.clickable {
                                    val nextHour = (hour + 1) % 24
                                    updateReminderTime(nextHour, minute, isReminderEnabled)
                                }.padding(8.dp),
                                fontSize = 18.sp,
                                color = GradientStart
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%02d", hour),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "▼",
                                modifier = Modifier.clickable {
                                    val nextHour = if (hour - 1 < 0) 23 else hour - 1
                                    updateReminderTime(nextHour, minute, isReminderEnabled)
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

                        // Minute controls
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "▲",
                                modifier = Modifier.clickable {
                                    val nextMin = (minute + 5) % 60
                                    updateReminderTime(hour, nextMin, isReminderEnabled)
                                }.padding(8.dp),
                                fontSize = 18.sp,
                                color = GradientStart
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%02d", minute),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "▼",
                                modifier = Modifier.clickable {
                                    val nextMin = if (minute - 5 < 0) 55 else minute - 5
                                    updateReminderTime(hour, nextMin, isReminderEnabled)
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

        // Topic picker setting
        val selectedTopic by settingsViewModel.selectedTopic.collectAsState()
        val topicName = com.nhimz.vocabmaster.ui.screens.AVAILABLE_TOPICS.find { it.first == selectedTopic }?.second ?: selectedTopic
        SettingsCard(title = "Chủ đề học tập") {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToTopicPicker() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Chủ đề hiện tại", fontSize = 14.sp)
                    Text(
                        text = "Các bài kiểm tra sẽ ưu tiên từ vựng trong chủ đề này",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = topicName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GradientStart,
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Theme & Language settings
        SettingsCard(title = "Hệ thống") {
            Column {
                // Theme selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Giao diện", fontSize = 14.sp)
                    Row {
                        listOf("LIGHT", "DARK", "SYSTEM").forEach { t ->
                            val isSelected = t == theme
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                                        } else {
                                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setTheme(t) }
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

                Spacer(modifier = Modifier.height(16.dp))

                // Language selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Ngôn ngữ dịch", fontSize = 14.sp)
                    Row {
                        listOf("VI", "EN").forEach { l ->
                            val isSelected = l == language
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                                        } else {
                                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setLanguage(l) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (l) {
                                        "VI" -> "Tiếng Việt"
                                        else -> "Tiếng Anh"
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

        // 5. Backup & Restore settings
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
                        onClick = {
                            exportBackupLauncher.launch("vocab_master_backup.json")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GradientStart
                        ),
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
                        onClick = {
                            importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
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

        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCard(title = "Developer") {
                Button(
                    onClick = onNavigateToDebugPanel,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text("Mở Debug Panel", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SettingsCard(
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
