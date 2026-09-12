package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.Source
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookAuthorDaoTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookAuthorDao: BookAuthorDao
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookAuthorDao = database.bookAuthorDao()

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
                    ),
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getAuthorsForBook returns every credited person via the join`() =
        runTest {
            val authorId = database.authorDao().insert(Author(name = "Jane Austen", birthYear = 1775, deathYear = 1817))
            val translatorId = database.authorDao().insert(Author(name = "Someone Translator", birthYear = null, deathYear = null))

            bookAuthorDao.insertAll(
                listOf(
                    BookAuthor(bookId = bookId, authorId = authorId, role = "author"),
                    BookAuthor(bookId = bookId, authorId = translatorId, role = "translator"),
                ),
            )

            val credited = bookAuthorDao.getAuthorsForBook(bookId)

            assertEquals(2, credited.size)
            assertTrue(credited.any { it.name == "Jane Austen" })
            assertTrue(credited.any { it.name == "Someone Translator" })
        }

    @Test
    fun `the same person can be credited in more than one role on the same book`() =
        runTest {
            val authorId = database.authorDao().insert(Author(name = "Multi Talented", birthYear = null, deathYear = null))

            bookAuthorDao.insertAll(
                listOf(
                    BookAuthor(bookId = bookId, authorId = authorId, role = "author"),
                    BookAuthor(bookId = bookId, authorId = authorId, role = "illustrator"),
                ),
            )

            val credited = bookAuthorDao.getAuthorsForBook(bookId)

            assertEquals(2, credited.size)
        }

    @Test
    fun `getAuthorsForBook returns an empty list for a book with no credited people yet`() =
        runTest {
            val credited = bookAuthorDao.getAuthorsForBook(bookId)

            assertTrue(credited.isEmpty())
        }
}
