package com.nhimz.vocabmaster.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class QuestionAndFsrsCard(
    @Embedded val question: QuestionEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val fsrsCard: FsrsCardEntity?
)
