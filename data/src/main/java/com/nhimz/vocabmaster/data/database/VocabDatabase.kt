package com.nhimz.vocabmaster.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.VocabularyCardEntity
import com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VocabularyCardEntity::class,
        ReviewLogEntity::class,
        FlaggedItemEntity::class
    ],
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS flagged_items (
                        word TEXT NOT NULL,
                        issueType TEXT NOT NULL,
                        details TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        PRIMARY KEY(word)
                    )
                """)
            }
        }
    }
}
