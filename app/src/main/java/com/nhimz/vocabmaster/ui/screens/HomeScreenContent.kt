package com.nhimz.vocabmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhimz.vocabmaster.ui.components.Duo3DCard

/**
 * Tracer slice (Plan 03-01, Task 1).
 *
 * Minimal stateless content that demonstrates the [com.nhimz.vocabmaster.ui.theme.VocabMasterTheme]
 * and [Duo3DCard] are correctly applied end-to-end. Renders a single 3D card on
 * a themed background so the rendering pipeline is exercised without depending
 * on the full Container state.
 *
 * Task 2 of Plan 03-01 will extend this file into the full HomeScreen content
 * by extracting the UI from `HomeScreen.kt`.
 */
@Composable
fun HomeScreenContentTracer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vocab Master",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Duo3DCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tracer card — Theme & Duo3DCard wired end-to-end.",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
