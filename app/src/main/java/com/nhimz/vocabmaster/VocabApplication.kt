package com.nhimz.vocabmaster

import android.app.Application
import com.nhimz.vocabmaster.util.LocalLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nhimz.vocabmaster.data.repository.SettingsRepositoryImpl

@HiltAndroidApp
class VocabApplication : Application() {

    @Inject
    lateinit var settingsRepositoryImpl: SettingsRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            LocalLogger.setupCrashHandler()
            LocalLogger.i("Application", "VocabMaster initialized in DEBUG mode.")
        }

        // Perform V6 DB Migration Reset
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("migration_state", MODE_PRIVATE)
            val lastKnownVersion = prefs.getInt("db_version", 5)
            if (lastKnownVersion < 6) {
                settingsRepositoryImpl.resetForMigrationV6()
                prefs.edit().putInt("db_version", 6).apply()
                LocalLogger.i("Application", "Performed resetForMigrationV6 settings wipe")
            }

            // One-time wipe of the legacy single-database file (`vocab_database`). The split-database
            // refactor (T06/MEM002) moved curriculum content into `curriculum_db` and per-user progress
            // into `user_data_db`; the old combined DB is orphaned and must be removed so a pre-existing
            // install starts cleanly against the two new databases.
            val legacyWiped = prefs.getBoolean("legacy_db_wiped", false)
            if (!legacyWiped) {
                deleteDatabase("vocab_database")
                prefs.edit().putBoolean("legacy_db_wiped", true).apply()
                LocalLogger.i("Application", "Removed legacy vocab_database file")
            }
        }
    }
}
