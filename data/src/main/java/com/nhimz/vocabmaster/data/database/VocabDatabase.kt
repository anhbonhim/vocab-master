package com.nhimz.vocabmaster.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import com.nhimz.vocabmaster.data.database.entity.UnitEntity
import com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity
import com.nhimz.vocabmaster.data.database.entity.NodeEntity
import com.nhimz.vocabmaster.data.database.entity.SessionEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity
import com.nhimz.vocabmaster.data.database.entity.SessionProgressEntity

/**
 * Room database for VocabMaster.
 *
 * Version 8 intentionally uses a destructive migration from v7 (see D-02).
 * Existing FSRS scheduling data is reset to [com.nhimz.vocabmaster.domain.fsrs.v6.State.New]
 * because the py-fsrs v6 port changes the card schema (step, stability, difficulty,
 * due/last_review millis). The production builder in DataModule already configures
 * `fallbackToDestructiveMigration(dropAllTables = true)`, so the v7→v8 bump wipes all
 * tables and the curriculum is re-seeded from assets on next access.
 */
@Database(
    entities = [
        FsrsCardEntity::class,
        ReviewLogEntity::class,
        FlaggedItemEntity::class,
        SectionEntity::class,
        UnitEntity::class,
        UnitGuidebookEntity::class,
        NodeEntity::class,
        SessionEntity::class,
        QuestionEntity::class,
        NodeProgressEntity::class,
        SessionProgressEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VocabDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao

    companion object {
        const val DATABASE_NAME = "vocab_master_db"
    }
}
