package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookSubject
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.local.entity.Subject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookSubjectDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookSubjectDao: BookSubjectDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookSubjectDao = database.bookSubjectDao()

            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1342",
                        title = "Pride and Prejudice",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-09-16",
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getSubjectsForBook returns every linked subject via the join`() =
        runTest {
            val fictionId = database.subjectDao().insert(Subject(label = "Fiction"))
            val romanceId = database.subjectDao().insert(Subject(label = "Romance"))

            bookSubjectDao.insertAll(
                listOf(
                    BookSubject(bookId = bookId, subjectId = fictionId),
                    BookSubject(bookId = bookId, subjectId = romanceId),
                ),
            )

            val subjects = bookSubjectDao.getSubjectsForBook(bookId)

            assertEquals(2, subjects.size)
        }

    @Test
    fun `getBooksForSubject returns every book linked to that subject`() =
        runTest {
            val fictionId = database.subjectDao().insert(Subject(label = "Fiction"))
            bookSubjectDao.insert(BookSubject(bookId = bookId, subjectId = fictionId))

            val books = bookSubjectDao.getBooksForSubject(fictionId)

            assertEquals(1, books.size)
            assertEquals("Pride and Prejudice", books.first().title)
        }

    @Test
    fun `getSubjectsForBook returns an empty list for an unclassified book`() =
        runTest {
            val subjects = bookSubjectDao.getSubjectsForBook(bookId)

            assertTrue(subjects.isEmpty())
        }

    @Test
    fun `deleteForBook clears every subject association, letting the sync client rebuild them fresh`() =
        runTest {
            val fictionId = database.subjectDao().insert(Subject(label = "Fiction"))
            bookSubjectDao.insert(BookSubject(bookId = bookId, subjectId = fictionId))

            bookSubjectDao.deleteForBook(bookId)

            val subjects = bookSubjectDao.getSubjectsForBook(bookId)
            assertTrue(subjects.isEmpty())
        }
}
