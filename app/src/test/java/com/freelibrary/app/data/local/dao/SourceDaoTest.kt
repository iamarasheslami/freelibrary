package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SourceDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var sourceDao: SourceDao

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()
        sourceDao = database.sourceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve a source by id`() =
        runTest {
            val source = Source(name = "Project Gutenberg", attribution = "Public domain works via Project Gutenberg")
            val insertedId = sourceDao.insert(source)

            val retrieved = sourceDao.getById(insertedId)

            assertEquals("Project Gutenberg", retrieved?.name)
            assertEquals("Public domain works via Project Gutenberg", retrieved?.attribution)
        }

    @Test
    fun `getById returns null for a source that does not exist`() =
        runTest {
            val retrieved = sourceDao.getById(999L)

            assertNull(retrieved)
        }

    @Test
    fun `getAll returns every inserted source`() =
        runTest {
            sourceDao.insert(Source(name = "Project Gutenberg", attribution = "Attribution A"))
            sourceDao.insert(Source(name = "Future Source", attribution = "Attribution B"))

            val all = sourceDao.getAll()

            assertEquals(2, all.size)
        }

    @Test
    fun `findByName locates an existing source to avoid duplicate inserts`() =
        runTest {
            sourceDao.insert(Source(name = "Project Gutenberg", attribution = "Attribution A"))

            val found = sourceDao.findByName("Project Gutenberg")

            assertEquals("Attribution A", found?.attribution)
        }

    @Test
    fun `findByName returns null for a source that has not been imported yet`() =
        runTest {
            val found = sourceDao.findByName("Nonexistent")

            assertNull(found)
        }
}
