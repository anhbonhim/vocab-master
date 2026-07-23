package com.nhimz.vocabmaster.domain.audio

interface AudioPlayer {
    fun playAudio(url: String?)
    fun stop()
    fun shutdown()
}
