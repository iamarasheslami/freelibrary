package com.freelibrary.app.di

import com.freelibrary.app.data.remote.CatalogSyncApi
import com.freelibrary.app.data.remote.createCatalogSyncApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideCatalogSyncApi(): CatalogSyncApi = createCatalogSyncApi()
}
