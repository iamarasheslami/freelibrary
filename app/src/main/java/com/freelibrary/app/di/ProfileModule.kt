package com.freelibrary.app.di

import android.content.Context
import coil.ImageLoader
import com.freelibrary.app.data.profile.AvatarStore
import com.freelibrary.app.data.profile.CoilAvatarDecoder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {
    @Provides
    @Singleton
    fun provideAvatarStore(
        @ApplicationContext context: Context,
        imageLoader: ImageLoader,
    ): AvatarStore = AvatarStore(File(context.filesDir, "profile"), CoilAvatarDecoder(context, imageLoader))
}
