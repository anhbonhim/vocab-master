package com.nhimz.vocabmaster.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhimz.vocabmaster.data.database.entity.NodeEntity
import com.nhimz.vocabmaster.data.database.entity.QuestionEntity
import com.nhimz.vocabmaster.data.database.entity.SectionEntity
import com.nhimz.vocabmaster.data.database.entity.SessionEntity
import com.nhimz.vocabmaster.data.database.entity.UnitEntity
import com.nhimz.vocabmaster.data.database.entity.UnitGuidebookEntity

/**
 * Read-only curriculum database.
 *
 * Holds static course content (sections, units, guidebooks, nodes, sessions, questions)
 * separate from per-user progress ([UserDataDatabase]). Version 1 starts fresh for the split
 * architecture (MEM002). It uses a destructive migration because curriculum content is fully
 * replaceable: on a curriculum update the whole database is wiped and re-seeded from the bundled
 * JSON asset, leaving user progress untouched. Queries that previously JOINed curriculum tables
 * with user-data tables now run as two single-DB queries joined in memory inside
 * [com.nhimz.vocabmaster.data.repository.VocabularyRepositoryImpl].
 */
@Database(
    entities = [
        SectionEntity::class,
        UnitEntity::class,
        UnitGuidebookEntity::class,
        NodeEntity::class,
        SessionEntity::class,
        QuestionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CurriculumDatabase : RoomDatabase() {
    abstract fun curriculumDao(): CurriculumDao

    companion object {
        const val DATABASE_NAME = "curriculum_db"
    }
}
