package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "nodes",
    indices = [Index(value = ["unitId", "index"], unique = true)]
)
data class NodeEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val index: Int,
    val type: Int, // NodeType ordinal
    val title: String,
    val scenarioContext: String,
    val icon: String
)
