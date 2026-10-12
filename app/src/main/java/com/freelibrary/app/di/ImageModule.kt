package com.freelibrary.app.di

import android.content.Context
import coil.ImageLoader
import com.freelibrary.app.ui.cover.createCoverImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {
    /** One loader for the whole app, so every screen shares the same capped memory and disk caches. */
    @Provides
    @Singleton
    fun provideCoverImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader = createCoverImageLoader(context)
}
