package com.nhimz.vocabmaster.ui.components.quiz

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrambledWordMapperTest {

    @Test
    fun testCalculateScrambledIndex_noDuplicates() {
        val scrambled = listOf("I", "love", "coding")
        val selected = listOf("I")

        val index = ScrambledWordMapper.calculateScrambledIndex("love", selected, scrambled)
        assertEquals(1, index)
    }

    @Test
    fun testCalculateScrambledIndex_withDuplicates() {
        val scrambled = listOf("a", "b", "a", "c", "a")
        
        // No "a" selected yet, should return first "a"
        var index = ScrambledWordMapper.calculateScrambledIndex("a", emptyList(), scrambled)
        assertEquals(0, index)

        // One "a" selected, should return second "a"
        index = ScrambledWordMapper.calculateScrambledIndex("a", listOf("a"), scrambled)
        assertEquals(2, index)

        // Two "a"s selected, should return third "a"
        index = ScrambledWordMapper.calculateScrambledIndex("a", listOf("a", "a"), scrambled)
        assertEquals(4, index)
    }

    @Test
    fun testCalculateSelectedIndices() {
        val scrambled = listOf("This", "is", "a", "test", "is", "it")
        val selected = listOf("is", "This", "is")

        val indices = ScrambledWordMapper.calculateSelectedIndices(selected, scrambled)
        
        // "is" (first) -> index 1
        // "This" -> index 0
        // "is" (second) -> index 4
        assertEquals(listOf(1, 0, 4), indices)
    }
}