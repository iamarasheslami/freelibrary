package com.freelibrary.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.TimeZone

/** The schema version of the database. Raise it together with a new migration in [MIGRATIONS]; a test enforces that. */
const val DATABASE_VERSION = 12

/**
 * The oldest schema version that may be on a reader's phone. Databases older than this only ever
 * existed on development phones, so they may be wiped; every later one must be migrated.
 */
const val FIRST_RELEASED_VERSION = 11

/**
 * Every migration from one schema version to the next, from [FIRST_RELEASED_VERSION] up. A reader's
 * shelves, reading places, bookmarks and downloads live in this database, so a schema change
 * must never wipe it: add a migration for each version bump, with its test.
 */
val MIGRATIONS: Array<Migration> = arrayOf(migration11To12())

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Version 12 adds the table of reading days behind the reading streak. The days a reader opened a book or finished one
 * before the update are filled in from what is already stored, so the streak does not start from nothing.
 */
fun migration11To12(zone: TimeZone = TimeZone.getDefault()): Migration =
    object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `reading_days` (`day` INTEGER NOT NULL, PRIMARY KEY(`day`))")
            val offset = zone.getOffset(System.currentTimeMillis())
            db.execSQL(
                """
                INSERT OR IGNORE INTO reading_days (day)
                SELECT (lastOpenedAt + $offset) / $MILLIS_PER_DAY FROM reading_progress
                UNION
                SELECT (finishedAt + $offset) / $MILLIS_PER_DAY FROM book_shelf_state WHERE finishedAt IS NOT NULL
                """.trimIndent(),
            )
        }
    }
