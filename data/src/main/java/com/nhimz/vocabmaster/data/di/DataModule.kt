package com.nhimz.vocabmaster.data.di

import android.content.Context
import androidx.room.Room
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
        @Provides
        @Singleton
        fun provideVocabDatabase(
            @ApplicationContext context: Context
        ): VocabDatabase {
            return Room.databaseBuilder(
                context,
                VocabDatabase::class.java,
                "vocab_database"
            ).addMigrations(VocabDatabase.MIGRATION_1_2, VocabDatabase.MIGRATION_2_3)
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
