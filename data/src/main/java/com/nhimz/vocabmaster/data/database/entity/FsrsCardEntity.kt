package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nhimz.vocabmaster.domain.fsrs.v6.Card
import com.nhimz.vocabmaster.domain.fsrs.v6.State

@Entity(tableName = "fsrs_cards")
data class FsrsCardEntity(
    @PrimaryKey val questionId: String,
    val due: Long,
    val stability: Double?,
    val difficulty: Double?,
    val step: Int?,
    val state: Int,
    val lastReview: Long?,
    val reps: Int,
    val lapses: Int
) {
    fun toDomain(): Card {
        return Card(
            cardId = questionId,
            due = due,
            stability = stability,
            difficulty = difficulty,
            step = step,
            state = State.entries.firstOrNull { it.value == state } ?: State.New,
            lastReview = lastReview,
            reps = reps,
            lapses = lapses
        )
    }

    companion object {
        fun fromDomain(card: Card): FsrsCardEntity {
            return FsrsCardEntity(
                questionId = card.cardId,
                due = card.due,
                stability = card.stability,
                difficulty = card.difficulty,
                step = card.step,
                state = card.state.value,
                lastReview = card.lastReview,
                reps = card.reps,
                lapses = card.lapses
            )
        }
    }
}
