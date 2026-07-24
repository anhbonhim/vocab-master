package com.nhimz.vocabmaster.domain.usecase

import com.nhimz.vocabmaster.domain.audio.AudioPlayer
import javax.inject.Inject

class AudioPlayerUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    fun playAudio(url: String?) {
        audioPlayer.playAudio(url)
    }

    fun stop() {
        audioPlayer.stop()
    }

    fun shutdown() {
        audioPlayer.shutdown()
    }
}
