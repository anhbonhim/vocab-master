package com.nhimz.vocabmaster.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nhimz.vocabmaster.MainActivity
import com.nhimz.vocabmaster.R
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var vocabularyRepository: VocabularyRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch a word to display
                var wordText = "persistent"
                var definitionText = "kiên trì, bền bỉ"

                val dueCards = vocabularyRepository.getDueCards(System.currentTimeMillis() / 1000, 5).firstOrNull()
                val card = dueCards?.randomOrNull() ?: vocabularyRepository.getCardsByLevel(DifficultyLevel.A2).firstOrNull()?.randomOrNull()

                if (card != null) {
                    wordText = card.vocabulary.word
                    definitionText = card.vocabulary.definition
                }

                showNotification(context, wordText, definitionText)
            } catch (e: Exception) {
                showNotification(context, "Vocab Master", "Time for your daily vocabulary review!")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, word: String, definition: String) {
        val channelId = "daily_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Word Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows the word of the day to help you learn consistently."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification) // App notification vector icon
            .setContentTitle("Từ vựng hôm nay: $word")
            .setContentText(definition)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Từ vựng hôm nay: **$word**\nÝ nghĩa: $definition\n\nBấm để bắt đầu học ngay!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
