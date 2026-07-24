package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "session_progress",
    indices = [Index(value = ["sessionId"], unique = true)]
)
data class SessionProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val isCompleted: Boolean,
    val completedAt: Long?
)
