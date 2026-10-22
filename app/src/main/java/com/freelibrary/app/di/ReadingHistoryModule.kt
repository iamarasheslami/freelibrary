package com.freelibrary.app.di

import com.freelibrary.app.data.repository.ReadingHistory
import com.freelibrary.app.data.repository.ReadingHistoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReadingHistoryModule {
    @Provides
    @Singleton
    fun provideReadingHistory(repository: ReadingHistoryRepository): ReadingHistory = repository
}
