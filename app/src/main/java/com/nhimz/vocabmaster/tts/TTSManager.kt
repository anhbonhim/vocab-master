package com.nhimz.vocabmaster.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : DefaultLifecycleObserver, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingTexts = mutableListOf<String>()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("TTSManager", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "US English language is not supported or missing data")
            } else {
                isInitialized = true
                Log.d("TTSManager", "TTS initialized successfully")
                // Play pending speech
                synchronized(pendingTexts) {
                    pendingTexts.forEach { speakInternal(it) }
                    pendingTexts.clear()
                }
            }
        } else {
            Log.e("TTSManager", "Initialization of TextToSpeech failed")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            speakInternal(text)
        } else {
            synchronized(pendingTexts) {
                if (pendingTexts.size < 5) { // Limit queue size
                    pendingTexts.add(text)
                }
            }
        }
    }

    private fun speakInternal(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VocabMasterTTSId")
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        shutdown()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.d("TTSManager", "TTS shut down")
    }
}
