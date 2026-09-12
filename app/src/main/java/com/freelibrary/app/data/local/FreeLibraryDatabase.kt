package com.freelibrary.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freelibrary.app.data.local.dao.AuthorDao
import com.freelibrary.app.data.local.dao.BookAuthorDao
import com.freelibrary.app.data.local.dao.BookBookshelfDao
import com.freelibrary.app.data.local.dao.BookDao
import com.freelibrary.app.data.local.dao.BookFormatDao
import com.freelibrary.app.data.local.dao.BookSubjectDao
import com.freelibrary.app.data.local.dao.BookshelfDao
import com.freelibrary.app.data.local.dao.SourceDao
import com.freelibrary.app.data.local.dao.SubjectDao
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.BookSubject
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.local.entity.Subject

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
    entities = [
        Source::class,
        Book::class,
        BookFormat::class,
        Author::class,
        BookAuthor::class,
        Subject::class,
        BookSubject::class,
        Bookshelf::class,
        BookBookshelf::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class FreeLibraryDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao

    abstract fun bookDao(): BookDao

    abstract fun bookFormatDao(): BookFormatDao

    abstract fun authorDao(): AuthorDao

    abstract fun bookAuthorDao(): BookAuthorDao

    abstract fun subjectDao(): SubjectDao

    abstract fun bookSubjectDao(): BookSubjectDao

    abstract fun bookshelfDao(): BookshelfDao

    abstract fun bookBookshelfDao(): BookBookshelfDao
}
