package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "sessions",
    indices = [Index(value = ["nodeId", "index"], unique = true)]
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val nodeId: String,
    val index: Int,
    val title: String,
    val durationMinutes: Int,
    val questionIds: String // JSON serialized List<String>
)
