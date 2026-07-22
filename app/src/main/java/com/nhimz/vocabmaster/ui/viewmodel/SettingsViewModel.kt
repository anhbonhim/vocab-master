package com.nhimz.vocabmaster.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.data.sync.SyncManager
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.ui.components.SnackbarMessage
import com.nhimz.vocabmaster.util.LocalLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val syncSuccess: Boolean? = null,
    val syncError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedTopic = MutableStateFlow("general")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * One-shot snackbar messages surfaced from settings operations (sync
     * failures, backup/restore errors). Container ([SettingsScreen]) reads
     * this flow and forwards each emission to the global
     * [androidx.compose.material3.SnackbarHostState] hosted in
     * [com.nhimz.vocabmaster.ui.VocabMasterApp].
     */
    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val snackbarMessages: SharedFlow<SnackbarMessage> = _snackbarMessages.asSharedFlow()

    private suspend fun emitSnackbar(message: SnackbarMessage) {
        _snackbarMessages.emit(message)
    }

    val dailyGoalXp: StateFlow<Int> = settingsRepository.dailyGoalXp
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 100)

    val theme: StateFlow<String> = settingsRepository.theme
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val language: StateFlow<String> = settingsRepository.language
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "EN")

    val desiredRetention: StateFlow<Double> = settingsRepository.desiredRetention
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0.90)

    fun setDailyGoal(xp: Int) {
        viewModelScope.launch { settingsRepository.updateDailyGoal(xp) }
    }

    fun setTheme(t: String) {
        viewModelScope.launch { settingsRepository.setTheme(t) }
    }

    fun setDesiredRetention(r: Double) {
        viewModelScope.launch { settingsRepository.setDesiredRetention(r) }
    }

    init {
        viewModelScope.launch {
            settingsRepository.selectedTopic.collect { topic ->
                _selectedTopic.value = topic
            }
        }
    }

    fun setSelectedTopic(topic: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedTopic(topic)
        }
    }

    fun triggerSync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncSuccess = null, syncError = null) }
            val success = syncManager.sync()
            if (success) {
                _uiState.update { it.copy(isSyncing = false, syncSuccess = true) }
                emitSnackbar(SnackbarMessage(text = "Đồng bộ hóa thành công!"))
            } else {
                val msg = "Đồng bộ hóa thất bại. Vui lòng kiểm tra kết nối mạng."
                _uiState.update { it.copy(isSyncing = false, syncSuccess = false, syncError = msg) }
                // Per D-01/D-05/D-07: surface a Snackbar with an inline Retry
                // action so the user can re-trigger the sync without leaving
                // the current screen.
                emitSnackbar(
                    SnackbarMessage(
                        text = msg,
                        actionLabel = "Thử lại",
                        isError = true,
                        action = { triggerSync() }
                    )
                )
            }
        }
    }

    fun backupData(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = backupRepository.exportBackup()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                onSuccess()
                emitSnackbar(SnackbarMessage(text = "Sao lưu dữ liệu thành công!"))
            } catch (e: Exception) {
                e.printStackTrace();
                val msg = e.localizedMessage ?: "Lỗi không xác định khi sao lưu."
                onError(msg)
                emitSnackbar(SnackbarMessage(text = "Sao lưu thất bại: $msg", isError = true))
            }
        }
    }

    fun restoreData(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonStringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            jsonStringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val jsonString = jsonStringBuilder.toString()
                backupRepository.importBackup(jsonString)
                    .onSuccess { success ->
                        if (success) {
                            onSuccess()
                            emitSnackbar(SnackbarMessage(text = "Khôi phục dữ liệu thành công!"))
                        } else {
                            val msg = "Dữ liệu sao lưu không hợp lệ hoặc bị lỗi."
                            onError(msg)
                            emitSnackbar(SnackbarMessage(text = msg, isError = true))
                        }
                    }
                    .onFailure { error ->
                        LocalLogger.e("SettingsViewModel", "Backup restore failed", error)
                        val msg = error.localizedMessage ?: "Lỗi không xác định khi khôi phục."
                        onError(msg)
                        emitSnackbar(SnackbarMessage(text = "Khôi phục thất bại: $msg", isError = true))
                    }
            } catch (e: java.io.IOException) {
                LocalLogger.e("SettingsViewModel", "Failed to read backup file", e)
                val msg = e.localizedMessage ?: "Lỗi không xác định khi khôi phục."
                onError(msg)
                emitSnackbar(SnackbarMessage(text = "Khôi phục thất bại: $msg", isError = true))
            }
        }
    }
}