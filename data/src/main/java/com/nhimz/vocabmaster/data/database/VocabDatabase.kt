package com.nhimz.vocabmaster.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.VocabularyCardEntity

@Database(
    entities = [
        VocabularyCardEntity::class,
        ReviewLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VocabDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao

    companion object {
        const val DATABASE_NAME = "vocab_master_db"
    }
}
