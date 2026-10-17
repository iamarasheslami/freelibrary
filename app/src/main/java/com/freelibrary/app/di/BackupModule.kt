package com.freelibrary.app.di

import android.content.Context
import com.freelibrary.app.data.backup.BookStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    /** Inside the app's private files, so no storage permission is needed and other apps cannot read the books. */
    @Provides
    @Singleton
    fun provideBookStorage(
        @ApplicationContext context: Context,
    ): BookStorage = BookStorage(File(context.filesDir, "books"))
}
