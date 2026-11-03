package com.freelibrary.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.TimeZone

/** Version 12 adds the reading days, filled in from the opens and finishes that are already stored. */
@RunWith(RobolectricTestRunner::class)
class MigrationTo12Test {
    private val day = 86_400_000L
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(null)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(11) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL("CREATE TABLE reading_progress (bookId INTEGER PRIMARY KEY, lastOpenedAt INTEGER)")
                                db.execSQL("CREATE TABLE book_shelf_state (bookId INTEGER PRIMARY KEY NOT NULL, finishedAt INTEGER)")
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
    }

    private fun days(): List<Long> {
        db.query("SELECT day FROM reading_days ORDER BY day").use { cursor ->
            val result = mutableListOf<Long>()
            while (cursor.moveToNext()) result += cursor.getLong(0)
            return result
        }
    }

    @Test
    fun `the days already stored become reading days, once each`() {
        val utc = TimeZone.getTimeZone("UTC")
        db.execSQL("INSERT INTO reading_progress VALUES (1, ${5 * day + 1_000}), (2, ${5 * day + 2_000}), (3, ${7 * day})")
        db.execSQL("INSERT INTO book_shelf_state VALUES (1, ${6 * day + 5}), (2, NULL)")

        migration11To12(utc).migrate(db)

        assertEquals(listOf(5L, 6L, 7L), days())
    }

    @Test
    fun `a reader with nothing stored starts with no reading days`() {
        migration11To12(TimeZone.getTimeZone("UTC")).migrate(db)

        assertEquals(emptyList<Long>(), days())
    }
}
