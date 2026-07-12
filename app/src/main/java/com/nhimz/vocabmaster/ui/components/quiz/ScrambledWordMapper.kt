package com.nhimz.vocabmaster.ui.components.quiz

object ScrambledWordMapper {
    /**
     * Finds the index of a word in the original scrambled list, taking into account
     * how many times that word has already been selected.
     */
    fun calculateScrambledIndex(
        word: String,
        selectedWords: List<String>,
        scrambledWords: List<String>
    ): Int {
        val occurrenceInSelected = selectedWords.count { it == word }
        var currentOccurrence = 0
        
        for (i in scrambledWords.indices) {
            if (scrambledWords[i] == word) {
                currentOccurrence++
                if (currentOccurrence > occurrenceInSelected) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Determines the selected indices from the scrambled list based on the currently
     * selected words list.
     */
    fun calculateSelectedIndices(
        selectedWords: List<String>,
        scrambledWords: List<String>
    ): List<Int> {
        val selectedIndices = mutableListOf<Int>()
        val selectedWordCounts = mutableMapOf<String, Int>()
        
        for (word in selectedWords) {
            val count = selectedWordCounts.getOrDefault(word, 0)
            selectedWordCounts[word] = count + 1
            
            var currentOccurrence = 0
            for (j in scrambledWords.indices) {
                if (scrambledWords[j] == word) {
                    currentOccurrence++
                    if (currentOccurrence == count + 1) {
                        selectedIndices.add(j)
                        break
                    }
                }
            }
        }
        return selectedIndices
    }
}