package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nhimz.vocabmaster.domain.fsrs.Rating
import com.nhimz.vocabmaster.domain.fsrs.ReviewLog
import com.nhimz.vocabmaster.domain.fsrs.State
import java.time.LocalDateTime

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cardId"])]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    
    // FSRS ReviewLog properties
    val rating: Rating,
    val elapsed_days: Int,
    val scheduled_days: Int,
    val stability: Double,
    val difficulty: Double,
    val state: State,
    val timestamp: LocalDateTime
) {
    fun toDomain(): ReviewLog {
        return ReviewLog(
            rating = rating,
            elapsed_days = elapsed_days,
            scheduled_days = scheduled_days,
            stability = stability,
            difficulty = difficulty,
            state = state,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(cardId: Long, log: ReviewLog): ReviewLogEntity {
            return ReviewLogEntity(
                cardId = cardId,
                rating = log.rating,
                elapsed_days = log.elapsed_days,
                scheduled_days = log.scheduled_days,
                stability = log.stability,
                difficulty = log.difficulty,
                state = log.state,
                timestamp = log.timestamp
            )
        }
    }
}
