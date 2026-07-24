package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val index: Int,
    val name: String,
    val cefrSublevel: String,
    val icon: String,
    val description: String
)
