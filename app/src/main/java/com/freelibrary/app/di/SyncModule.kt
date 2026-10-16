package com.freelibrary.app.di

import com.freelibrary.app.data.repository.CatalogSyncRunner
import com.freelibrary.app.data.repository.CatalogSyncer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    @Provides
    @Singleton
    fun provideCatalogSyncer(runner: CatalogSyncRunner): CatalogSyncer = runner
}
