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

/** Version 13 adds the withdrawn-books table and leaves every existing row alone. */
@RunWith(RobolectricTestRunner::class)
class MigrationTo13Test {
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
                        object : SupportSQLiteOpenHelper.Callback(12) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL("CREATE TABLE books (id INTEGER PRIMARY KEY NOT NULL, title TEXT)")
                                db.execSQL("INSERT INTO books VALUES (1, 'kept')")
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

    private fun count(table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    @Test
    fun `the table is created empty and existing books stay`() {
        migration12To13().migrate(db)

        assertEquals(0, count("withdrawn_books"))
        assertEquals(1, count("books"))
        db.execSQL("INSERT INTO withdrawn_books VALUES (1)")
        assertEquals(1, count("withdrawn_books"))
    }
}
