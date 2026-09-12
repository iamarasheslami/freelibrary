package com.freelibrary.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freelibrary.app.data.local.dao.SourceDao
import com.freelibrary.app.data.local.entity.Source

/**
 * The app's single Room database. Entities and DAOs are added here as each
 * table is built out; version is bumped and a migration written whenever the
 * schema changes after release.
 */
@Database(
    entities = [Source::class],
    version = 1,
    exportSchema = true,
)
abstract class FreeLibraryDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
}
