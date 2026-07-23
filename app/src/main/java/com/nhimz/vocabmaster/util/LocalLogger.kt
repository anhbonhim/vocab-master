package com.nhimz.vocabmaster.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val tag: String,
    val message: String
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
        
    override fun toString(): String {
        return "[$formattedTime] $level/$tag: $message"
    }
}

object LocalLogger {
    private const val MAX_LOG_COUNT = 500
    private val _logs = MutableStateFlow<List<LogEvent>>(emptyList())
    val logs: StateFlow<List<LogEvent>> = _logs.asStateFlow()

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        appendLog(LogEvent(level = "D", tag = tag, message = message))
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        appendLog(LogEvent(level = "I", tag = tag, message = message))
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        appendLog(LogEvent(level = "W", tag = tag, message = message))
    }

    fun section(tag: String, title: String) {
        i(tag, "═══ [$title] ═══")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) "$message\n${throwable.stackTraceToString()}" else message
        appendLog(LogEvent(level = "E", tag = tag, message = fullMessage))
    }

    private fun appendLog(event: LogEvent) {
        _logs.update { currentLogs ->
            val newLogs = currentLogs + event
            if (newLogs.size > MAX_LOG_COUNT) {
                newLogs.drop(newLogs.size - MAX_LOG_COUNT)
            } else {
                newLogs
            }
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun getExportString(): String {
        val builder = java.lang.StringBuilder()
        builder.appendLine("=== Vocab Master Debug Logs ===")
        builder.appendLine("Exported at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        builder.appendLine("===============================")
        _logs.value.forEach { event ->
            builder.appendLine(event.toString())
        }
        return builder.toString()
    }
    
    fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("CRASH_HANDLER", "App crashed on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
