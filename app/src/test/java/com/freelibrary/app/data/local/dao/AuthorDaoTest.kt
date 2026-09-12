package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthorDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var authorDao: AuthorDao

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()
        authorDao = database.authorDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve an author with birth and death years`() =
        runTest {
            val id = authorDao.insert(Author(name = "Jane Austen", birthYear = 1775, deathYear = 1817))

            val retrieved = authorDao.getById(id)

            assertEquals("Jane Austen", retrieved?.name)
            assertEquals(1775, retrieved?.birthYear)
            assertEquals(1817, retrieved?.deathYear)
        }

    @Test
    fun `an author with unknown birth and death years can be stored as null`() =
        runTest {
            val id = authorDao.insert(Author(name = "Anonymous", birthYear = null, deathYear = null))

            val retrieved = authorDao.getById(id)

            assertNull(retrieved?.birthYear)
            assertNull(retrieved?.deathYear)
        }

    @Test
    fun `findByName locates an existing author to avoid duplicate inserts`() =
        runTest {
            authorDao.insert(Author(name = "Jane Austen", birthYear = 1775, deathYear = 1817))

            val found = authorDao.findByName("Jane Austen")

            assertEquals(1775, found?.birthYear)
        }

    @Test
    fun `findByName returns null for an author that has not been imported yet`() =
        runTest {
            val found = authorDao.findByName("Nobody Yet")

            assertNull(found)
        }
}
