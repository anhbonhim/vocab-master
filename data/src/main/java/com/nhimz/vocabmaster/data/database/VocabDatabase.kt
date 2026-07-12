package com.nhimz.vocabmaster.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.VocabularyCardEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VocabularyCardEntity::class,
        ReviewLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VocabDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao

    companion object {
        const val DATABASE_NAME = "vocab_master_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary_cards ADD COLUMN topic TEXT NOT NULL DEFAULT 'general'")
                db.execSQL("ALTER TABLE vocabulary_cards ADD COLUMN audioUrl TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE vocabulary_cards ADD COLUMN scrambledSentenceData TEXT DEFAULT NULL")
            }
        }
    }
}
