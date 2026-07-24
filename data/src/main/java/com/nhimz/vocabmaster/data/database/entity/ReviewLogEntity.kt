package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nhimz.vocabmaster.domain.fsrs.v6.Rating
import com.nhimz.vocabmaster.domain.fsrs.v6.ReviewLog

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = FsrsCardEntity::class,
            parentColumns = ["questionId"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cardId"])]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,

    // FSRS-6 ReviewLog properties
    val rating: Int,
    val reviewDatetime: Long,
    val reviewDuration: Long?
) {
    fun toDomain(): ReviewLog {
        return ReviewLog(
            cardId = cardId,
            rating = Rating.entries.firstOrNull { it.value == rating } ?: Rating.Good,
            reviewDatetime = reviewDatetime,
            reviewDuration = reviewDuration
        )
    }

    companion object {
        fun fromDomain(log: ReviewLog): ReviewLogEntity {
            return ReviewLogEntity(
                cardId = log.cardId,
                rating = log.rating.value,
                reviewDatetime = log.reviewDatetime,
                reviewDuration = log.reviewDuration
            )
        }
    }
}
