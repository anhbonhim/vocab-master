package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.model.VocabularyItem
import javax.inject.Inject

class GenerateDistractorsUseCase @Inject constructor() {
    /**
     * Rule-based logic to select 3 incorrect options from the vocabulary database.
     * Prioritizes distractors that:
     * 1. Match both the part of speech AND difficulty level
     * 2. Match the part of speech
     * 3. Match the difficulty level
     * 4. Any other items
     */
    fun execute(
        correctItem: VocabularyItem,
        allVocabulary: List<VocabularyItem>,
        count: Int = 3
    ): List<VocabularyItem> {
        val pool = allVocabulary.filter { it.id != correctItem.id }

        val samePosAndLevel = pool.filter {
            it.partOfSpeech.equals(correctItem.partOfSpeech, ignoreCase = true) &&
                    it.difficultyLevel == correctItem.difficultyLevel
        }

        val samePos = pool.filter {
            it.partOfSpeech.equals(correctItem.partOfSpeech, ignoreCase = true) &&
                    it.difficultyLevel != correctItem.difficultyLevel
        }

        val sameLevel = pool.filter {
            !it.partOfSpeech.equals(correctItem.partOfSpeech, ignoreCase = true) &&
                    it.difficultyLevel == correctItem.difficultyLevel
        }

        val others = pool.filter {
            !it.partOfSpeech.equals(correctItem.partOfSpeech, ignoreCase = true) &&
                    it.difficultyLevel != correctItem.difficultyLevel
        }

        val sortedPool = samePosAndLevel.shuffled() +
                samePos.shuffled() +
                sameLevel.shuffled() +
                others.shuffled()

        return sortedPool.take(count)
    }
}
