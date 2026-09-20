package com.freelibrary.app.di

import android.content.Context
import androidx.room.Room
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.dao.AuthorDao
import com.freelibrary.app.data.local.dao.BookAuthorDao
import com.freelibrary.app.data.local.dao.BookBookshelfDao
import com.freelibrary.app.data.local.dao.BookDao
import com.freelibrary.app.data.local.dao.BookFormatDao
import com.freelibrary.app.data.local.dao.BookFtsDao
import com.freelibrary.app.data.local.dao.BookSubjectDao
import com.freelibrary.app.data.local.dao.BookmarkDao
import com.freelibrary.app.data.local.dao.BookshelfDao
import com.freelibrary.app.data.local.dao.DownloadedBookDao
import com.freelibrary.app.data.local.dao.HighlightDao
import com.freelibrary.app.data.local.dao.ReadingProgressDao
import com.freelibrary.app.data.local.dao.SourceDao
import com.freelibrary.app.data.local.dao.SubjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "catalog.sqlite"

/**
 * Provides the app's single Room database instance and every DAO derived
 * from it. The database is loaded from the bundled asset (fetched via the
 * fetchCatalogDatabase Gradle task at build time) using createFromAsset,
 * giving every install a complete, searchable catalog with zero network
 * requests needed on first launch.
 *
 * fallbackToDestructiveMigration is used deliberately pre-release: no real
 * user data exists yet to preserve across schema changes. This must be
 * replaced with real Migration objects before any public release.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): FreeLibraryDatabase {
        return Room.databaseBuilder(context, FreeLibraryDatabase::class.java, DATABASE_NAME)
            .createFromAsset(DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSourceDao(database: FreeLibraryDatabase): SourceDao = database.sourceDao()

    @Provides
    fun provideBookDao(database: FreeLibraryDatabase): BookDao = database.bookDao()

    @Provides
    fun provideBookFormatDao(database: FreeLibraryDatabase): BookFormatDao = database.bookFormatDao()

    @Provides
    fun provideAuthorDao(database: FreeLibraryDatabase): AuthorDao = database.authorDao()

    @Provides
    fun provideBookAuthorDao(database: FreeLibraryDatabase): BookAuthorDao = database.bookAuthorDao()

    @Provides
    fun provideSubjectDao(database: FreeLibraryDatabase): SubjectDao = database.subjectDao()

    @Provides
    fun provideBookSubjectDao(database: FreeLibraryDatabase): BookSubjectDao = database.bookSubjectDao()

    @Provides
    fun provideBookshelfDao(database: FreeLibraryDatabase): BookshelfDao = database.bookshelfDao()

    @Provides
    fun provideBookBookshelfDao(database: FreeLibraryDatabase): BookBookshelfDao = database.bookBookshelfDao()

    @Provides
    fun provideReadingProgressDao(database: FreeLibraryDatabase): ReadingProgressDao = database.readingProgressDao()

    @Provides
    fun provideDownloadedBookDao(database: FreeLibraryDatabase): DownloadedBookDao = database.downloadedBookDao()

    @Provides
    fun provideBookmarkDao(database: FreeLibraryDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideHighlightDao(database: FreeLibraryDatabase): HighlightDao = database.highlightDao()

    @Provides
    fun provideBookFtsDao(database: FreeLibraryDatabase): BookFtsDao = database.bookFtsDao()
}
