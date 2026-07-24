package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flagged_items")
data class FlaggedItemEntity(
    @PrimaryKey val questionId: String,
    val word: String?, // For display fallback
    val issueType: String, // "AUDIO_ISSUE", "DATA_ISSUE"
    val details: String,
    val timestamp: Long
)
