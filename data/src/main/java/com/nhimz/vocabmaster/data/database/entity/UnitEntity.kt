package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "units",
    indices = [Index(value = ["sectionId", "index"], unique = true)]
)
data class UnitEntity(
    @PrimaryKey val id: String,
    val sectionId: String,
    val index: Int,
    val topic: String,
    val title: String,
    val storySummary: String,
    val icon: String,
    val guidebookId: String
)
