package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "node_progress",
    indices = [Index(value = ["nodeId"], unique = true)]
)
data class NodeProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nodeId: String,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val accuracy: Float?,
    val bestScore: Int?
)
