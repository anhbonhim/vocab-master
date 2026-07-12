package com.nhimz.vocabmaster.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

@Serializable
data class UserSettingsBackup(
    val currentStreak: Int,
    val longestStreak: Int,
    val availableFreezes: Int,
    val lastStudyDate: Long,
    val xpTotal: Int,
    val badgeStatus: List<String>,
    val dailyGoalMinutes: Int,
    val desiredRetention: Double,
    val theme: String,
    val language: String
)

@Serializable
data class VocabularyCardBackup(
    val id: Long,
    val word: String,
    val definition: String,
    val partOfSpeech: String,
    val difficultyLevel: String,
    val example: String?,
    val ipa: String?,
    @Serializable(with = LocalDateTimeSerializer::class) val due: LocalDateTime,
    val stability: Double,
    val difficulty: Double,
    val interval: Int,
    val reps: Int,
    val lapses: Int,
    val state: String,
    @Serializable(with = LocalDateTimeSerializer::class) val lastReview: LocalDateTime?,
    val topic: String? = "general",
    val audioUrl: String? = null,
    val scrambledSentenceData: String? = null
)

@Serializable
data class ReviewLogBackup(
    val id: Long,
    val cardId: Long,
    val rating: String,
    val elapsed_days: Int,
    val scheduled_days: Int,
    val stability: Double,
    val difficulty: Double,
    val state: String,
    @Serializable(with = LocalDateTimeSerializer::class) val timestamp: LocalDateTime
)

@Serializable
data class BackupPayload(
    val userSettings: UserSettingsBackup,
    val vocabularyCards: List<VocabularyCardBackup>,
    val reviewLogs: List<ReviewLogBackup>
)
