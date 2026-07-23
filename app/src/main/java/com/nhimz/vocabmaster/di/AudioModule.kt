package com.nhimz.vocabmaster.di

import com.nhimz.vocabmaster.audio.CDNAudioPlayer
import com.nhimz.vocabmaster.domain.audio.AudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(
        impl: CDNAudioPlayer
    ): AudioPlayer
}
