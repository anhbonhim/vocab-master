package com.nhimz.vocabmaster.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.domain.fsrs.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.nhimz.vocabmaster.util.LocalLogger
import java.io.File
import java.io.FileWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelScreen(
    onBack: () -> Unit,
    cdnAudioPlayer: CDNAudioPlayer,
    vocabDatabase: VocabDatabase
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Audio Cache", "DB & FSRS", "Logs")

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
                        text = { Text(title) }
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
                    1 -> DatabaseFsrsTab(vocabDatabase.vocabDao())
                    2 -> LogsTab()
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
fun DatabaseFsrsTab(vocabDao: VocabDao) {
    val scope = rememberCoroutineScope()
    var statsMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalCards by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            totalCards = vocabDao.getCardCount()
            val newCards = vocabDao.getCardCountByState(State.New.name)
            val learningCards = vocabDao.getCardCountByState(State.Learning.name)
            val reviewCards = vocabDao.getCardCountByState(State.Review.name)
            val relearningCards = vocabDao.getCardCountByState(State.Relearning.name)
            
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
