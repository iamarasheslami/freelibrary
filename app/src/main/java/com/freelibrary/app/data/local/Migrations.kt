package com.freelibrary.app.data.local

import androidx.room.migration.Migration

/** The schema version of the database. Raise it together with a new migration in [MIGRATIONS]; a test enforces that. */
const val DATABASE_VERSION = 11

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
val MIGRATIONS: Array<Migration> = emptyArray()
