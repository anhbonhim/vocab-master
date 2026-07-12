package com.nhimz.vocabmaster

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.notification.NotificationScheduler
import com.nhimz.vocabmaster.ui.VocabMasterApp
import com.nhimz.vocabmaster.ui.viewmodel.FlashcardViewModel
import com.nhimz.vocabmaster.ui.viewmodel.MainViewModel
import com.nhimz.vocabmaster.ui.viewmodel.PlacementTestViewModel
import com.nhimz.vocabmaster.ui.viewmodel.QuizViewModel
import com.nhimz.vocabmaster.ui.viewmodel.SettingsViewModel
import com.nhimz.vocabmaster.ui.viewmodel.StatisticsViewModel
import com.nhimz.vocabmaster.data.database.VocabDatabase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var cdnAudioPlayer: CDNAudioPlayer

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var vocabDatabase: VocabDatabase

    private val mainViewModel: MainViewModel by viewModels()
    private val placementTestViewModel: PlacementTestViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()
    private val flashcardViewModel: FlashcardViewModel by viewModels()
    private val statisticsViewModel: StatisticsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind TTSManager to the activity lifecycle
        lifecycle.addObserver(cdnAudioPlayer)

        initializeAppDefaultSettings()

        setContent {
            VocabMasterApp(
                mainViewModel = mainViewModel,
                placementTestViewModel = placementTestViewModel,
                quizViewModel = quizViewModel,
                flashcardViewModel = flashcardViewModel,
                statisticsViewModel = statisticsViewModel,
                settingsViewModel = settingsViewModel,
                cdnAudioPlayer = cdnAudioPlayer,
                notificationScheduler = notificationScheduler,
                vocabDatabase = vocabDatabase
            )
        }
    }

    private fun initializeAppDefaultSettings() {
        // Schedule default daily reminder (e.g. 9:00 AM) if it hasn't been set before
        val sharedPrefs: SharedPreferences = getSharedPreferences("reminder_prefs", MODE_PRIVATE)
        if (!sharedPrefs.contains("reminder_hour")) {
            sharedPrefs.edit()
                .putInt("reminder_hour", 9)
                .putInt("reminder_minute", 0)
                .putBoolean("reminder_enabled", true)
                .apply()
            notificationScheduler.scheduleDailyNotification(9, 0)
        }
    }
}
