package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Bookshelf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookshelfDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookshelfDao: BookshelfDao

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()
        bookshelfDao = database.bookshelfDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve a bookshelf by id`() =
        runTest {
            val id = bookshelfDao.insert(Bookshelf(name = "Science Fiction"))

            val retrieved = bookshelfDao.getById(id)

            assertEquals("Science Fiction", retrieved?.name)
        }

    @Test
    fun `findByName locates an existing bookshelf to avoid duplicate inserts`() =
        runTest {
            bookshelfDao.insert(Bookshelf(name = "Children's Literature"))

            val found = bookshelfDao.findByName("Children's Literature")

            assertEquals("Children's Literature", found?.name)
        }

    @Test
    fun `findByName returns null for a bookshelf that has not been imported yet`() =
        runTest {
            val found = bookshelfDao.findByName("Nonexistent")

            assertNull(found)
        }

    @Test
    fun `getAll returns every bookshelf in alphabetical order`() =
        runTest {
            bookshelfDao.insertAll(
                listOf(
                    Bookshelf(name = "Science Fiction"),
                    Bookshelf(name = "Adventure"),
                    Bookshelf(name = "Mystery"),
                ),
            )

            val all = bookshelfDao.getAll()

            assertEquals(listOf("Adventure", "Mystery", "Science Fiction"), all.map { it.name })
        }
}
