package com.nhimz.vocabmaster.data.database

import androidx.room.TypeConverter
import com.nhimz.vocabmaster.domain.model.NodeType
import com.nhimz.vocabmaster.domain.model.QuestionType

class Converters {
    @TypeConverter
    fun toNodeType(value: Int): NodeType {
        return NodeType.entries.getOrNull(value) ?: NodeType.LESSON
    }

    @TypeConverter
    fun fromNodeType(nodeType: NodeType): Int {
        return nodeType.ordinal
    }

    @TypeConverter
    fun toQuestionType(value: Int): QuestionType {
        return QuestionType.entries.getOrNull(value) ?: QuestionType.FILL_IN_BLANK
    }

    @TypeConverter
    fun fromQuestionType(questionType: QuestionType): Int {
        return questionType.ordinal
    }
}
