package com.nhimz.vocabmaster.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhimz.vocabmaster.domain.model.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

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
                e.printStackTrace()
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
                e.printStackTrace()
                onError(e.localizedMessage ?: "Lỗi không xác định khi khôi phục.")
            }
        }
    }
}
