package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookSummary
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.BookAuthorCredit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDetailsRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: BookDetailsRepository
    private var sourceId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository =
                BookDetailsRepository(database.bookDao(), database.bookAuthorDao(), database.bookSummaryDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun addBook(
        externalId: String = "1342",
        title: String = "Pride and Prejudice",
        coverUrl: String? = "https://example.org/1342.cover.medium.jpg",
    ): Long =
        database.bookDao().insert(
            Book(
                sourceId = sourceId,
                externalId = externalId,
                title = title,
                issuedDate = "2026-09-01",
                primaryLanguage = "en",
                locc = null,
                lastModified = "2026-10-04",
                coverUrl = coverUrl,
            ),
        )

    private suspend fun addAuthor(
        name: String,
        birthYear: Int? = null,
        deathYear: Int? = null,
    ): Long = database.authorDao().insert(Author(name = name, birthYear = birthYear, deathYear = deathYear))

    private suspend fun credit(
        bookId: Long,
        authorId: Long,
        role: String = "author",
    ) {
        database.bookAuthorDao().insert(BookAuthor(bookId = bookId, authorId = authorId, role = role))
    }

    @Test
    fun `a book that does not exist gives nothing`() =
        runTest {
            assertNull(repository.load(999))
        }

    @Test
    fun `the page carries the book's own details`() =
        runTest {
            val bookId = addBook()

            val details = repository.load(bookId)!!

            assertEquals(bookId, details.bookId)
            assertEquals("1342", details.externalId)
            assertEquals("Pride and Prejudice", details.title)
            assertEquals("https://example.org/1342.cover.medium.jpg", details.coverUrl)
            assertEquals("en", details.language)
        }

    @Test
    fun `authors come in credit order with their years`() =
        runTest {
            val bookId = addBook()
            val first = addAuthor("Austen, Jane", 1775, 1817)
            val second = addAuthor("Brock, C. E.", 1870, 1938)
            credit(bookId, second, role = "illustrator")
            credit(bookId, first)

            val details = repository.load(bookId)!!

            assertEquals(
                listOf(BookAuthorCredit("Brock, C. E.", 1870, 1938), BookAuthorCredit("Austen, Jane", 1775, 1817)),
                details.authors,
            )
        }

    @Test
    fun `a person credited in two roles appears once`() =
        runTest {
            val bookId = addBook()
            val person = addAuthor("Austen, Jane")
            credit(bookId, person, role = "author")
            credit(bookId, person, role = "editor")

            assertEquals(1, repository.load(bookId)!!.authors.size)
        }

    @Test
    fun `the summary is carried when there is one`() =
        runTest {
            val bookId = addBook()
            database.bookSummaryDao().upsert(BookSummary(bookId, "A story of manners."))

            assertEquals("A story of manners.", repository.load(bookId)!!.summary)
        }

    @Test
    fun `a missing or blank summary counts as no summary`() =
        runTest {
            val withoutSummary = addBook("1", "No summary")
            val blank = addBook("2", "Blank summary")
            database.bookSummaryDao().upsert(BookSummary(blank, "   "))

            assertNull(repository.load(withoutSummary)!!.summary)
            assertNull(repository.load(blank)!!.summary)
        }

    @Test
    fun `the card form uses the first credited author and the same cover`() =
        runTest {
            val bookId = addBook()
            credit(bookId, addAuthor("Austen, Jane"))

            val card = repository.load(bookId)!!.toCard()

            assertEquals("Austen, Jane", card.authorName)
            assertEquals("https://example.org/1342.cover.medium.jpg", card.coverUrl)
            assertEquals(bookId, card.bookId)
        }

    @Test
    fun `a book with no credits has no author on its card`() =
        runTest {
            val bookId = addBook()

            assertNull(repository.load(bookId)!!.toCard().authorName)
        }
}
