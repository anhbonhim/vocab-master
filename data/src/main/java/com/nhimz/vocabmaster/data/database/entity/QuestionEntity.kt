package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["type"])
    ]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val word: String?,
    val type: Int, // QuestionType ordinal
    val prompt: String,
    val options: String?, // JSON serialized List<String>
    val correctIndex: Int?,
    val correctSentence: String?,
    val scrambledWords: String?, // JSON serialized List<String>
    val translation: String?,
    val audioUrl: String?,
    val audioUrlSlow: String?,
    val matchingPairs: String?, // JSON serialized List<MatchPair>
    val imagePath: String?
)
