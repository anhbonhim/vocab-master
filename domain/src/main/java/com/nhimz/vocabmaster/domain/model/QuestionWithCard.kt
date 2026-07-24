package com.nhimz.vocabmaster.domain.model

import com.nhimz.vocabmaster.domain.fsrs.v6.Card

data class QuestionWithCard(
    val question: Question,
    val card: Card
)
