package com.nhimz.vocabmaster.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.data.sync.SyncManager
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncSuccess = null, syncError = null) }
            val success = syncManager.sync()
            if (success) {
                _uiState.update { it.copy(isSyncing = false, syncSuccess = true) }
            } else {
                _uiState.update { it.copy(isSyncing = false, syncSuccess = false, syncError = "Đồng bộ hóa thất bại. Vui lòng kiểm tra kết nối mạng.") }
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
            } catch (e: Exception) {
                e.printStackTrace();
                onError(e.localizedMessage ?: "Lỗi không xác định khi sao lưu.")
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
                val success = backupRepository.importBackup(jsonString)
                if (success) {
                    onSuccess()
                } else {
                    onError("Dữ liệu sao lưu không hợp lệ hoặc bị lỗi.")
                }
            } catch (e: Exception) {
                e.printStackTrace();
                onError(e.localizedMessage ?: "Lỗi không xác định khi khôi phục.")
            }
        }
    }
}