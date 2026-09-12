package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Subject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubjectDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var subjectDao: SubjectDao

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()
        subjectDao = database.subjectDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve a subject by id`() =
        runTest {
            val id = subjectDao.insert(Subject(label = "Fiction"))

            val retrieved = subjectDao.getById(id)

            assertEquals("Fiction", retrieved?.label)
        }

    @Test
    fun `findByLabel locates an existing subject to avoid duplicate inserts`() =
        runTest {
            subjectDao.insert(Subject(label = "Science"))

            val found = subjectDao.findByLabel("Science")

            assertEquals("Science", found?.label)
        }

    @Test
    fun `findByLabel returns null for a subject that has not been imported yet`() =
        runTest {
            val found = subjectDao.findByLabel("Nonexistent")

            assertNull(found)
        }
}
