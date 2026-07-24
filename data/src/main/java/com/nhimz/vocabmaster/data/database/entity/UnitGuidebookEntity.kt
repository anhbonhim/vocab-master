package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "unit_guidebooks",
    indices = [Index(value = ["unitId"], unique = true)]
)
data class UnitGuidebookEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val grammarTips: String, // JSON serialized List<String>
    val keyPhrases: String, // JSON serialized List<KeyPhrase>
    val storyIntro: String,
    val illustrationSvg: String?
)
