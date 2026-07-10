package com.nhimz.vocabmaster.domain.model

import com.nhimz.vocabmaster.domain.fsrs.Card

data class VocabularyItemWithCard(
    val vocabulary: VocabularyItem,
    val card: Card
)
