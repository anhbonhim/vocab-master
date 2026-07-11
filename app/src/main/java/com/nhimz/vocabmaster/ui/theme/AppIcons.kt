package com.nhimz.vocabmaster.ui.theme

import androidx.compose.runtime.Composable
import com.nhimz.vocabmaster.R

/**
 * Central registry for all custom vector assets used in the application.
 * Use [androidx.compose.material.icons.Icons] for standard action icons.
 */
object AppIcons {
    // Example: The app launcher logo (can be used in About screen, Splash screen)
    val Logo: Int @Composable get() = R.drawable.ic_launcher_foreground

    // Example: Notification bell/V icon
    val Notification: Int @Composable get() = R.drawable.ic_stat_notification
    
    // Add future custom SVG/XML vectors here...
}