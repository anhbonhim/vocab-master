package com.nhimz.vocabmaster.data.di

import android.content.Context
import androidx.room.Room
import com.nhimz.vocabmaster.data.database.CurriculumDatabase
import com.nhimz.vocabmaster.data.database.CurriculumDao
import com.nhimz.vocabmaster.data.database.UserDataDatabase
import com.nhimz.vocabmaster.data.database.UserDataDao
import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.data.database.VocabDatabase
import com.nhimz.vocabmaster.data.repository.ReviewRepositoryImpl
import com.nhimz.vocabmaster.data.repository.SettingsRepositoryImpl
import com.nhimz.vocabmaster.data.repository.VocabularyRepositoryImpl
import com.nhimz.vocabmaster.domain.model.ReviewRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import com.nhimz.vocabmaster.domain.model.VocabularyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.nhimz.vocabmaster.data.repository.BackupRepositoryImpl
import com.nhimz.vocabmaster.domain.model.BackupRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindVocabularyRepository(
        impl: VocabularyRepositoryImpl
    ): VocabularyRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        impl: ReviewRepositoryImpl
    ): ReviewRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    companion object {
        // ---- New split databases (T03): CurriculumDb + UserDataDb ----
        // The legacy VocabDatabase/VocabDao providers below are kept temporarily until
        // T04 (VocabularyRepositoryImpl) and T05 (ReviewRepositoryImpl, BackupRepositoryImpl)
        // migrate off them. Remove VocabDatabase/VocabDao once those migrations land.

        @Provides
        @Singleton
        fun provideCurriculumDatabase(@ApplicationContext context: Context): CurriculumDatabase {
            return Room.databaseBuilder(
                context,
                CurriculumDatabase::class.java,
                CurriculumDatabase.DATABASE_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
        }

        @Provides
        fun provideCurriculumDao(database: CurriculumDatabase): CurriculumDao = database.curriculumDao()

        @Provides
        @Singleton
        fun provideUserDataDatabase(@ApplicationContext context: Context): UserDataDatabase {
            return Room.databaseBuilder(
                context,
                UserDataDatabase::class.java,
                UserDataDatabase.DATABASE_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
        }

        @Provides
        fun provideUserDataDao(database: UserDataDatabase): UserDataDao = database.userDataDao()

        @Provides
        @Singleton
        /**
         * Provides the VocabDatabase instance.
         * The builder deliberately omits main-thread query permission so Room's built-in guard throws
         * IllegalStateException on any non-suspend/non-Flow DAO call, enforcing PERS-02 at runtime.
         * Main-thread allowance is permitted ONLY in test in-memory builders.
         */
        fun provideVocabDatabase(
            @ApplicationContext context: Context
        ): VocabDatabase {
            return Room.databaseBuilder(
                context,
                VocabDatabase::class.java,
                "vocab_database"
            )
             .fallbackToDestructiveMigration(dropAllTables = true)
             .build()
        }

        @Provides
        @Singleton
        fun provideVocabDao(database: VocabDatabase): VocabDao {
            return database.vocabDao()
        }
    }
}
