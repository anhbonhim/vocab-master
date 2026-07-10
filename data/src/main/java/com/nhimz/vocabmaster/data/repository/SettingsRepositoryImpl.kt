package com.nhimz.vocabmaster.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val AVAILABLE_FREEZES = intPreferencesKey("available_freezes")
        val LAST_STUDY_DATE = longPreferencesKey("last_study_date")
        val XP_TOTAL = intPreferencesKey("xp_total")
        val BADGE_STATUS = stringPreferencesKey("badge_status")
        val DESIRED_RETENTION = doublePreferencesKey("desired_retention")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
    }

    private val dataStore = context.dataStore

    override val dailyGoalMinutes: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 5
        }

    override suspend fun setDailyGoalMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_GOAL_MINUTES] = minutes
        }
    }

    override val currentStreak: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.CURRENT_STREAK] ?: 0
        }

    override suspend fun setCurrentStreak(streak: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_STREAK] = streak
        }
    }

    override val longestStreak: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.LONGEST_STREAK] ?: 0
        }

    override suspend fun setLongestStreak(streak: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LONGEST_STREAK] = streak
        }
    }

    override val availableFreezes: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.AVAILABLE_FREEZES] ?: 1
        }

    override suspend fun setAvailableFreezes(freezes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AVAILABLE_FREEZES] = freezes
        }
    }

    override val lastStudyDate: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.LAST_STUDY_DATE] ?: 0L
        }

    override suspend fun setLastStudyDate(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_STUDY_DATE] = timestamp
        }
    }

    override val xpTotal: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.XP_TOTAL] ?: 0
        }

    override suspend fun addXp(xp: Int) {
        dataStore.edit { preferences ->
            val currentXp = preferences[PreferencesKeys.XP_TOTAL] ?: 0
            preferences[PreferencesKeys.XP_TOTAL] = currentXp + xp
        }
    }

    override suspend fun setXpTotal(xp: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.XP_TOTAL] = xp
        }
    }

    override val badgeStatus: Flow<List<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val serialized = preferences[PreferencesKeys.BADGE_STATUS] ?: ""
            if (serialized.isEmpty()) emptyList() else serialized.split(",")
        }

    override suspend fun addBadge(badge: String) {
        dataStore.edit { preferences ->
            val serialized = preferences[PreferencesKeys.BADGE_STATUS] ?: ""
            val currentList = if (serialized.isEmpty()) emptyList() else serialized.split(",")
            if (!currentList.contains(badge)) {
                val newList = currentList + badge
                preferences[PreferencesKeys.BADGE_STATUS] = newList.joinToString(",")
            }
        }
    }

    override suspend fun setBadgeStatus(badges: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BADGE_STATUS] = badges.joinToString(",")
        }
    }

    override val desiredRetention: Flow<Double> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.DESIRED_RETENTION] ?: 0.9
        }

    override suspend fun setDesiredRetention(retention: Double) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DESIRED_RETENTION] = retention
        }
    }

    override val theme: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.THEME] ?: "SYSTEM"
        }

    override suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    override val language: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "VI"
        }

    override suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }
}
