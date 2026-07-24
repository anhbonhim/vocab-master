package com.nhimz.vocabmaster.domain.model

fun Question.displayTitle(): String {
    return word ?: prompt
}
