package com.freelibrary.app.di

import android.content.Context
import com.freelibrary.app.data.remote.CatalogSyncApi
import com.freelibrary.app.data.remote.createCatalogHttpClient
import com.freelibrary.app.data.remote.createCatalogSyncApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideCatalogSyncApi(
        @ApplicationContext context: Context,
    ): CatalogSyncApi = createCatalogSyncApi(client = createCatalogHttpClient(userAgent = appUserAgent(context)))
}
