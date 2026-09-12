package com.freelibrary.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freelibrary.app.data.local.dao.BookDao
import com.freelibrary.app.data.local.dao.BookFormatDao
import com.freelibrary.app.data.local.dao.SourceDao
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.Source

/**
 * The app's single Room database. Entities and DAOs are added here as each
 * table is built out; version is bumped and a migration written whenever the
 * schema changes after release.
 *
 * Pre-release note: schema version bumps currently rely on
 * fallbackToDestructiveMigration() at the database-builder call site, since no
 * shipped user data exists yet to preserve. Real Migration objects must replace
 * this before any public release.
 */
@Database(
    entities = [Source::class, Book::class, BookFormat::class],
    version = 2,
    exportSchema = true,
)
abstract class FreeLibraryDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao

    abstract fun bookDao(): BookDao

    abstract fun bookFormatDao(): BookFormatDao
}
