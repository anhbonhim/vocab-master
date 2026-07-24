package com.nhimz.vocabmaster.ui.screens.debug_components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.util.LocalLogger
import kotlin.system.measureTimeMillis

enum class TestStatus {
    NOT_RUN,
    RUNNING,
    PASS,
    WARN,
    FAIL
}

data class AssertionResult(
    val description: String,
    val passed: Boolean,
    val message: String? = null
)

data class TestResult(
    val name: String,
    val description: String,
    val status: TestStatus = TestStatus.NOT_RUN,
    val details: String = "",
    val durationMs: Long = 0,
    val assertions: List<AssertionResult> = emptyList()
)

data class TestGroup(
    val title: String,
    val tests: List<TestResult>
)

suspend fun runTest(
    name: String,
    description: String,
    block: suspend (StringBuilder, MutableList<AssertionResult>) -> TestStatus
): TestResult {
    val logBuilder = StringBuilder()
    val assertions = mutableListOf<AssertionResult>()
    LocalLogger.i("TestRunner", "Starting test: $name")
    logBuilder.appendLine("Starting test: $name")
    
    var status = TestStatus.NOT_RUN
    val elapsed = measureTimeMillis {
        try {
            status = block(logBuilder, assertions)
        } catch (e: Throwable) {
            status = TestStatus.FAIL
            logBuilder.appendLine("Exception during test execution: ${e.message}")
            logBuilder.appendLine(e.stackTraceToString())
            assertions.add(
                AssertionResult(
                    description = "Test executed without exception",
                    passed = false,
                    message = e.message ?: e.javaClass.simpleName
                )
            )
        }
    }
    
    val finalStatus = if (status == TestStatus.PASS && assertions.any { !it.passed }) {
        TestStatus.FAIL
    } else {
        status
    }
    
    val details = logBuilder.toString()
    LocalLogger.i("TestRunner", "Finished test: $name in ${elapsed}ms with status: $finalStatus")
    
    when (finalStatus) {
        TestStatus.PASS -> LocalLogger.i("TestRunner", "Test passed: $name")
        TestStatus.WARN -> LocalLogger.w("TestRunner", "Test finished with warning: $name")
        TestStatus.FAIL -> LocalLogger.e("TestRunner", "Test failed: $name\nDetails:\n$details")
        else -> {}
    }
    
    return TestResult(
        name = name,
        description = description,
        status = finalStatus,
        details = details,
        durationMs = elapsed,
        assertions = assertions
    )
}

@Composable
fun StatusBadge(status: TestStatus) {
    val containerColor = when (status) {
        TestStatus.NOT_RUN -> MaterialTheme.colorScheme.surfaceVariant
        TestStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
        TestStatus.PASS -> Color(0xFFE8F5E9) // Light green
        TestStatus.WARN -> Color(0xFFFFF3E0) // Light orange
        TestStatus.FAIL -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (status) {
        TestStatus.NOT_RUN -> MaterialTheme.colorScheme.onSurfaceVariant
        TestStatus.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
        TestStatus.PASS -> Color(0xFF2E7D32) // Dark green
        TestStatus.WARN -> Color(0xFFE65100) // Dark orange
        TestStatus.FAIL -> MaterialTheme.colorScheme.onErrorContainer
    }
    
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TestResultCard(
    result: TestResult,
    onRunClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = result.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(status = result.status)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (result.status != TestStatus.NOT_RUN && result.status != TestStatus.RUNNING) {
                        Text(
                            text = "Duration: ${result.durationMs} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onRunClick,
                        enabled = result.status != TestStatus.RUNNING,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (result.status == TestStatus.RUNNING) "Running" else "Run",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    if (result.status != TestStatus.NOT_RUN) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand"
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(visible = expanded && result.status != TestStatus.NOT_RUN) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    if (result.assertions.isNotEmpty()) {
                        Text(
                            text = "Assertions:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        result.assertions.forEach { assertion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dotColor = if (assertion.passed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(dotColor, shape = RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = assertion.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!assertion.passed && !assertion.message.isNullOrEmpty()) {
                                        Text(
                                            text = assertion.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (result.details.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Console logs:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = result.details,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
