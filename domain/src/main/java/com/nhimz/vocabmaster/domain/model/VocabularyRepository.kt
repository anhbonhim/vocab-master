package com.nhimz.vocabmaster.domain.model

import com.nhimz.vocabmaster.domain.fsrs.Card
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    fun getDueCards(currentTimestamp: Long, limit: Int): Flow<List<VocabularyItemWithCard>>
    fun getDueCardsByTopic(topic: String, currentTimestamp: Long, limit: Int): Flow<List<VocabularyItemWithCard>>
    fun getCardsByLevel(level: DifficultyLevel): Flow<List<VocabularyItemWithCard>>
    fun getCardsByTopic(topic: String): Flow<List<VocabularyItemWithCard>>
    suspend fun getCardById(id: Long): VocabularyItemWithCard?
    suspend fun updateCard(card: Card)
    suspend fun insertCard(word: VocabularyItem, card: Card): Long
    suspend fun insertAll(items: List<VocabularyItemWithCard>)
    suspend fun getCount(): Int
}
