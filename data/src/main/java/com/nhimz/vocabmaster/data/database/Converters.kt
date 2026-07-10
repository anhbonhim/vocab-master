package com.nhimz.vocabmaster.data.database

import androidx.room.TypeConverter
import com.nhimz.vocabmaster.domain.fsrs.Rating
import com.nhimz.vocabmaster.domain.fsrs.State
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(ZoneId.systemDefault())?.toEpochSecond()
    }

    @TypeConverter
    fun toState(value: Int): State {
        return State.entries.firstOrNull { it.value == value } ?: State.New
    }

    @TypeConverter
    fun fromState(state: State): Int {
        return state.value
    }

    @TypeConverter
    fun toRating(value: Int): Rating {
        return Rating.entries.firstOrNull { it.value == value } ?: Rating.Good
    }

    @TypeConverter
    fun fromRating(rating: Rating): Int {
        return rating.value
    }
}
