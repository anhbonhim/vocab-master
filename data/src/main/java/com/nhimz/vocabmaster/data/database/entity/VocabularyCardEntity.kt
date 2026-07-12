package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nhimz.vocabmaster.domain.fsrs.Card
import com.nhimz.vocabmaster.domain.fsrs.State
import com.nhimz.vocabmaster.domain.model.DifficultyLevel
import com.nhimz.vocabmaster.domain.model.VocabularyItem
import com.nhimz.vocabmaster.domain.model.VocabularyItemWithCard
import java.time.LocalDateTime

@Entity(tableName = "vocabulary_cards")
data class VocabularyCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val definition: String,
    val partOfSpeech: String,
    val difficultyLevel: String, // "A1", "A2", "B1", "B2", "C1", "C2"
    val example: String?,
    val ipa: String?,
    
    // FSRS Card properties
    val due: LocalDateTime,
    val stability: Double,
    val difficulty: Double,
    val interval: Int,
    val reps: Int,
    val lapses: Int,
    val state: State,
    val lastReview: LocalDateTime?,
    
    // Extended properties
    val topic: String = "general",
    val audioUrl: String? = null,
    val scrambledSentenceData: String? = null
) {
    fun toDomain(): VocabularyItemWithCard {
        return VocabularyItemWithCard(
            vocabulary = VocabularyItem(
                id = id.toString(),
                word = word,
                definition = definition,
                partOfSpeech = partOfSpeech,
                difficultyLevel = DifficultyLevel.valueOf(difficultyLevel),
                example = example,
                ipa = ipa,
                topic = topic,
                audioUrl = audioUrl,
                scrambledSentenceData = scrambledSentenceData
            ),
            card = Card(
                id = id,
                due = due,
                stability = stability,
                difficulty = difficulty,
                interval = interval,
                reps = reps,
                lapses = lapses,
                state = state,
                lastReview = lastReview
            )
        )
    }

    companion object {
        fun fromDomain(vocab: VocabularyItem, card: Card): VocabularyCardEntity {
            return VocabularyCardEntity(
                id = card.id,
                word = vocab.word,
                definition = vocab.definition,
                partOfSpeech = vocab.partOfSpeech,
                difficultyLevel = vocab.difficultyLevel.name,
                example = vocab.example,
                ipa = vocab.ipa,
                due = card.due,
                stability = card.stability,
                difficulty = card.difficulty,
                interval = card.interval,
                reps = card.reps,
                lapses = card.lapses,
                state = card.state,
                lastReview = card.lastReview,
                topic = vocab.topic,
                audioUrl = vocab.audioUrl,
                scrambledSentenceData = vocab.scrambledSentenceData
            )
        }
    }
}
