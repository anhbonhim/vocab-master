package com.nhimz.vocabmaster.ui.screens

import android.os.Environment
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.data.database.CurriculumDao
import com.nhimz.vocabmaster.data.database.UserDataDao
import com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nhimz.vocabmaster.util.LocalLogger
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelScreen(
    onBack: () -> Unit,
    cdnAudioPlayer: CDNAudioPlayer,
    curriculumDao: CurriculumDao,
    userDataDao: UserDataDao,
    vocabularyRepository: VocabularyRepository,
    reviewRepository: ReviewRepository,
    settingsRepository: SettingsRepository,
    backupRepository: BackupRepository
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Audio Cache", "DB & FSRS", "Logs", "Dataset QA", "Audio QA Studio", "Flagged Items")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Debug Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 10.sp) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> AudioCacheTab(cdnAudioPlayer)
                    1 -> DatabaseFsrsTab(userDataDao)
                    2 -> LogsTab()
                    3 -> DatasetQATab(curriculumDao)
                    4 -> AudioQAStudioTab(curriculumDao, userDataDao, cdnAudioPlayer, settingsRepository)
                    5 -> FlaggedItemsTab(userDataDao)
                }
            }
        }
    }
}

@Composable
fun AudioCacheTab(cdnAudioPlayer: CDNAudioPlayer) {
    var urlInput by remember { mutableStateOf("https://cdn.jsdelivr.net/gh/nhimz/vocab-master-audio@main/audio/example.ogg") }
    var cacheStatus by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Trạng thái ExoPlayer Cache", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Nhập Audio CDN URL") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                cacheStatus = cdnAudioPlayer.isAudioCached(urlInput)
            }) {
                Text("Kiểm tra Cache")
            }
            
            Button(onClick = {
                cdnAudioPlayer.playAudio(urlInput)
            }) {
                Text("Phát thử")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (cacheStatus != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (cacheStatus == true) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = if (cacheStatus == true) "HIT (Tồn tại trong Cache cục bộ)" else "MISS (Chưa được cache, cần mạng để tải)",
                        color = if (cacheStatus == true) Color(0xFF065F46) else Color(0xFF991B1B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DatabaseFsrsTab(userDataDao: UserDataDao) {
    val scope = rememberCoroutineScope()
    var statsMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalCards by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            totalCards = userDataDao.getCardCount()
            val newCards = userDataDao.getCardCountByState(State.New.value)
            val learningCards = userDataDao.getCardCountByState(State.Learning.value)
            val reviewCards = userDataDao.getCardCountByState(State.Review.value)
            val relearningCards = userDataDao.getCardCountByState(State.Relearning.value)
            
            withContext(Dispatchers.Main) {
                statsMap = mapOf(
                    "New" to newCards,
                    "Learning" to learningCards,
                    "Review" to reviewCards,
                    "Relearning" to relearningCards
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Thống kê Database Raw", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Tổng số thẻ: $totalCards", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(statsMap.toList()) { (state, count) ->
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(state, fontWeight = FontWeight.Medium)
                        Text("$count cards", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun LogsTab() {
    val context = LocalContext.current
    val logs by LocalLogger.logs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Runtime Logs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Total logs: ${logs.size}", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                try {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val logFile = File(downloadsDir, "vocab_master_debug_logs_${System.currentTimeMillis()}.txt")
                    val writer = FileWriter(logFile)
                    writer.append(LocalLogger.getExportString())
                    writer.flush()
                    writer.close()
                    Toast.makeText(context, "Đã xuất log ra: ${logFile.absolutePath}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi xuất log: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) {
                Text("Xuất Logs to Downloads")
            }
            Button(onClick = { LocalLogger.clear() }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Clear")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs.reversed()) { logEvent ->
                val color = when (logEvent.level) {
                    "E" -> Color.Red
                    "W" -> Color(0xFFF59E0B) // Amber
                    "I" -> Color(0xFF3B82F6) // Blue
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = logEvent.toString(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun DatasetQATab(curriculumDao: CurriculumDao) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("Chưa thực hiện quét") }
    val anomalies = remember { mutableStateListOf<String>() }
    // Fix: cast to SnapshotStateList to resolve type parameter inference
    val anomaliesList = anomalies as SnapshotStateList<String>

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Dataset Automated Auditor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tự động phân tích toàn bộ thẻ từ vựng trong SQLite Database để tìm lỗi logic cấu trúc JSON và dữ liệu rỗng.",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    isScanning = true
                    anomaliesList.clear()
                    scope.launch(Dispatchers.IO) {
                        try {
                            val cards = curriculumDao.getAllQuestions()
                            val total = cards.size
                            cards.forEachIndexed { index, card ->
                                if ((index + 1) % 100 == 0 || index == total - 1) {
                                    withContext(Dispatchers.Main) {
                                        progressText = "Đang kiểm tra: ${index + 1}/$total từ..."
                                    }
                                }

                                // 1. Kiểm tra JSON scrambledSentenceData
                                val scrambledData = card.scrambledWords
                                if (scrambledData.isNullOrBlank()) {
                                    anomaliesList.add("[ERR_JSON_EMPTY] '${card.id}': scrambledSentenceData bị trống hoặc null")
                                } else {
                                    try {
                                        val array = jsonArrayToList(scrambledData)
                                        if (array.isEmpty()) {
                                            anomaliesList.add("[WARN_JSON_ARRAY] '${card.id}': scrambledSentenceData là mảng rỗng []")
                                        }
                                    } catch (e: Exception) {
                                        anomaliesList.add("[ERR_JSON_FORMAT] '${card.id}': scrambledSentenceData lỗi cú pháp JSON: ${e.message}")
                                    }
                                }

                                // 2. Kiểm tra audioUrl rỗng
                                if (card.audioUrl.isNullOrBlank()) {
                                    anomaliesList.add("[ERR_AUDIO_URL] '${card.id}': audioUrl bị trống")
                                }

                            }
                            withContext(Dispatchers.Main) {
                                isScanning = false
                                progressText = "Hoàn tất quét! Phát hiện ${anomaliesList.size} cảnh báo."
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isScanning = false
                                progressText = "Lỗi trong quá trình quét: ${e.message}"
                            }
                        }
                    }
                },
                enabled = !isScanning
            ) {
                Text(if (isScanning) "Đang Quét..." else "Bắt đầu Quét")
            }

            Button(
                onClick = { anomaliesList.clear(); progressText = "Đã dọn dẹp kết quả" },
                enabled = !isScanning && anomaliesList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Dọn dẹp")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(progressText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(anomaliesList) { anomaly ->
                val color = if (anomaly.contains("ERR")) Color(0xFFEF4444) else Color(0xFFF59E0B)
                Text(
                    text = anomaly,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = color,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun AudioQAStudioTab(
    curriculumDao: CurriculumDao,
    userDataDao: UserDataDao,
    cdnAudioPlayer: CDNAudioPlayer,
    settingsRepository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var cardsList by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var useLocalServer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val list = curriculumDao.getAllQuestions()
            val localSetting = settingsRepository.useLocalDevServer.first()
            withContext(Dispatchers.Main) {
                cardsList = list
                useLocalServer = localSetting
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Audio QA Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Stream qua Local Python Server", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Bật khi đang chạy server Termux (port 8080) chứa ogg gốc.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Switch(
                checked = useLocalServer,
                onCheckedChange = { enabled ->
                    useLocalServer = enabled
                    scope.launch { settingsRepository.setUseLocalDevServer(enabled) }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cardsList.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Đang tải danh sách từ vựng...")
            }
        } else {
            val currentCard = cardsList.getOrNull(currentIndex)
            if (currentCard != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                            )
                            Text(
                                text = "${currentIndex + 1} / ${cardsList.size}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentCard.id,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentCard.translation ?: "",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { cdnAudioPlayer.playAudio(currentCard.audioUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("🔊 Phát Âm Thanh")
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (currentIndex > 0) currentIndex--
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Trước")
                    }

                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                userDataDao.insertFlaggedItem(
                                    FlaggedItemEntity(
                                        questionId = currentCard.id,
                                        word = currentCard.prompt,
                                        issueType = "AUDIO_ISSUE",
                                        details = "Tester phát hiện âm thanh lỗi (rè, méo, hoặc rỗng)",
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Đã cắm cờ báo lỗi từ: ${currentCard.id}", Toast.LENGTH_SHORT).show()
                                    if (currentIndex < cardsList.size - 1) currentIndex++
                                }
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("🚩 BÁO LỖI (FLAG)")
                    }

                    Button(
                        onClick = {
                            if (currentIndex < cardsList.size - 1) currentIndex++
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Đạt -> Tiếp")
                    }
                }
            } else {
                Text("Không có dữ liệu thẻ từ")
            }
        }
    }
}

@Composable
fun FlaggedItemsTab(userDataDao: UserDataDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var flaggedList by remember { mutableStateOf<List<FlaggedItemEntity>>(emptyList()) }

    fun refreshFlagged() {
        scope.launch(Dispatchers.IO) {
            val list = userDataDao.getAllFlaggedItems()
            withContext(Dispatchers.Main) {
                flaggedList = list
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshFlagged()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Danh sách cắm cờ (${flaggedList.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        try {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val reportFile = File(downloadsDir, "flagged_assets_report_${System.currentTimeMillis()}.json")
                            val writer = FileWriter(reportFile)
                            
                            val finalJson = "[" + flaggedList.joinToString(",") { 
                                "{\"word\":\"${it.questionId}\",\"issue\":\"${it.issueType}\",\"details\":\"${it.details}\"}"
                            } + "]"
                                
                            writer.append(finalJson)
                            writer.flush()
                            writer.close()
                            Toast.makeText(context, "Đã xuất báo cáo ra: ${reportFile.absolutePath}", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi xuất báo cáo: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = flaggedList.isNotEmpty()
                ) {
                    Text("Xuất JSON")
                }
                
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            userDataDao.deleteAllFlaggedItems()
                            refreshFlagged()
                        }
                    },
                    enabled = flaggedList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa hết")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (flaggedList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Chưa phát hiện từ vựng nào bị lỗi", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(flaggedList) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.questionId, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(item.details, fontSize = 12.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        userDataDao.deleteFlaggedItem(item.questionId)
                                        refreshFlagged()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.5f), contentColor = Color.DarkGray)
                            ) {
                                Text("Bỏ cờ", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun jsonArrayToList(jsonArrayStr: String): List<String> {
    return jsonArrayStr
        .removePrefix("[")
        .removeSuffix("]")
        .split(",")
        .map { it.trim().removePrefix("\"").removeSuffix("\"") }
        .filter { it.isNotEmpty() }
}
