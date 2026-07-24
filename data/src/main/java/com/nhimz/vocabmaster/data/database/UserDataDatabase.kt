package com.nhimz.vocabmaster.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhimz.vocabmaster.data.database.entity.FlaggedItemEntity
import com.nhimz.vocabmaster.data.database.entity.FsrsCardEntity
import com.nhimz.vocabmaster.data.database.entity.NodeProgressEntity
import com.nhimz.vocabmaster.data.database.entity.ReviewLogEntity
import com.nhimz.vocabmaster.data.database.entity.SessionProgressEntity

/**
 * Per-user progress database.
 *
 * Holds user-generated data (FSRS scheduling cards, review logs, node/session progress, flagged
 * items) separate from the static curriculum ([CurriculumDatabase]). Version 1 starts fresh for
 * the split architecture (MEM002). It uses a destructive migration because this database is wiped
 * once on the first launch after the upgrade (the one-time data wipe documented in MEM002); after
 * that, curriculum updates replace [CurriculumDatabase] without touching this data, so user
 * progress survives every subsequent curriculum bump.
 *
 * Every query in [UserDataDao] touches only user-data tables. The 8 queries that previously JOINed
 * user-data tables with curriculum tables now run as two single-DB queries joined in memory inside
 * [com.nhimz.vocabmaster.data.repository.VocabularyRepositoryImpl]; the curriculum side of each
 * join is fetched from [CurriculumDao]. The cross-DB helper queries here (e.g.
 * [UserDataDao.getCardsByQuestionIds], [UserDataDao.getDueAndNewCardsByQuestionIds],
 * [UserDataDao.getCompletedNodeProgressByNodeIds]) filter the user-data tables by the ID lists
 * supplied by [CurriculumDao] so the repository can assemble the final result in memory.
 */
@Database(
    entities = [
        FsrsCardEntity::class,
        ReviewLogEntity::class,
        NodeProgressEntity::class,
        SessionProgressEntity::class,
        FlaggedItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UserDataDatabase : RoomDatabase() {
    abstract fun userDataDao(): UserDataDao

    companion object {
        const val DATABASE_NAME = "user_data_db"
    }
}
