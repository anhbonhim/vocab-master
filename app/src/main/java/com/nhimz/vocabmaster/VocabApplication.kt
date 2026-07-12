package com.nhimz.vocabmaster

import android.app.Application
import com.nhimz.vocabmaster.util.LocalLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VocabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            LocalLogger.setupCrashHandler()
            LocalLogger.i("Application", "VocabMaster initialized in DEBUG mode.")
        }
    }
}
