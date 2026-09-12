package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
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
class BookDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookDao: BookDao
    private lateinit var sourceDao: SourceDao
    private var sourceId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookDao = database.bookDao()
            sourceDao = database.sourceDao()

            sourceId = sourceDao.insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and retrieve a book by id`() =
        runTest {
            val book =
                Book(
                    sourceId = sourceId,
                    externalId = "1342",
                    title = "Pride and Prejudice",
                    issuedDate = "1998-06-01",
                    primaryLanguage = "en",
                    locc = "PR",
                )
            val insertedId = bookDao.insert(book)

            val retrieved = bookDao.getById(insertedId)

            assertEquals("Pride and Prejudice", retrieved?.title)
            assertEquals("1342", retrieved?.externalId)
        }

    @Test
    fun `getBySourceAndExternalId finds an existing book`() =
        runTest {
            bookDao.insert(
                Book(
                    sourceId = sourceId,
                    externalId = "1342",
                    title = "Pride and Prejudice",
                    issuedDate = null,
                    primaryLanguage = "en",
                    locc = null,
                ),
            )

            val found = bookDao.getBySourceAndExternalId(sourceId, "1342")

            assertEquals("Pride and Prejudice", found?.title)
        }

    @Test
    fun `getBySourceAndExternalId returns null when not found`() =
        runTest {
            val found = bookDao.getBySourceAndExternalId(sourceId, "does-not-exist")

            assertNull(found)
        }

    @Test
    fun `nullable fields can genuinely be null`() =
        runTest {
            val book =
                Book(
                    sourceId = sourceId,
                    externalId = "999",
                    title = "Untitled Work",
                    issuedDate = null,
                    primaryLanguage = null,
                    locc = null,
                )
            val insertedId = bookDao.insert(book)

            val retrieved = bookDao.getById(insertedId)

            assertNull(retrieved?.issuedDate)
            assertNull(retrieved?.primaryLanguage)
            assertNull(retrieved?.locc)
        }

    @Test
    fun `count reflects number of inserted books`() =
        runTest {
            bookDao.insertAll(
                listOf(
                    Book(sourceId = sourceId, externalId = "1", title = "Book One", issuedDate = null, primaryLanguage = null, locc = null),
                    Book(sourceId = sourceId, externalId = "2", title = "Book Two", issuedDate = null, primaryLanguage = null, locc = null),
                ),
            )

            assertEquals(2, bookDao.count())
        }
}
