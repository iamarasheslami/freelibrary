package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookFts
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.shared.SearchQueryPlanner
import com.freelibrary.shared.SearchScope
import com.freelibrary.shared.SearchText
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookFtsDaoCandidatesTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookFtsDao: BookFtsDao
    private var sourceId: Long = 0

    private suspend fun addBook(
        externalId: String,
        title: String,
        vararg authorNames: String,
    ): Long {
        val bookId =
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = externalId,
                    title = title,
                    issuedDate = null,
                    primaryLanguage = "en",
                    locc = null,
                    lastModified = "2026-10-02",
                ),
            )
        authorNames.forEach { name ->
            val authorId = database.authorDao().insert(Author(name = name, birthYear = null, deathYear = null))
            database.bookAuthorDao().insert(BookAuthor(bookId = bookId, authorId = authorId, role = "author"))
        }
        bookFtsDao.upsert(
            BookFts(
                bookId = bookId,
                title = SearchText.fold(title),
                authorNames = SearchText.fold(authorNames.joinToString(" ")),
            ),
        )
        return bookId
    }

    private fun matchQuery(
        query: String,
        scope: SearchScope,
    ) = SearchQueryPlanner.plan(query, scope)!!.matchQuery

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookFtsDao = database.bookFtsDao()
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `findCandidates returns one row per credited person in credit order`() =
        runTest {
            val bookId = addBook("1", "The Gambler", "Dostoyevsky, Fyodor", "Hogarth, C. J.")

            val rows = bookFtsDao.findCandidates(matchQuery("gambler", SearchScope.TITLE), limit = 10)

            assertEquals(listOf(bookId, bookId), rows.map { it.bookId })
            assertEquals(listOf("Dostoyevsky, Fyodor", "Hogarth, C. J."), rows.map { it.authorName })
            assertEquals("The Gambler", rows.first().title)
            assertEquals("1", rows.first().externalId)
        }

    @Test
    fun `a book with no credits still comes back, with a null author`() =
        runTest {
            addBook("2", "Anonymous Tales")

            val rows = bookFtsDao.findCandidates(matchQuery("tales", SearchScope.TITLE), limit = 10)

            assertEquals(1, rows.size)
            assertNull(rows.first().authorName)
        }

    @Test
    fun `the limit counts books, not author rows`() =
        runTest {
            addBook("1", "Tales One", "Author A", "Author B", "Author C")
            addBook("2", "Tales Two", "Author D")
            addBook("3", "Tales Three", "Author E")

            val rows = bookFtsDao.findCandidates(matchQuery("tales", SearchScope.TITLE), limit = 2)

            assertEquals(2, rows.map { it.bookId }.distinct().size)
        }

    @Test
    fun `the planner's expressions work against the real index, with accents and prefixes`() =
        runTest {
            addBook("135", "Les Misérables", "Hugo, Victor")
            addBook("767", "Jane Eyre", "Brontë, Charlotte")

            val byTitle = bookFtsDao.findCandidates(matchQuery("MISERAB", SearchScope.TITLE), limit = 10)
            val byAuthor = bookFtsDao.findCandidates(matchQuery("bronte charlotte", SearchScope.AUTHOR), limit = 10)

            assertEquals(listOf("135"), byTitle.map { it.externalId })
            assertEquals(listOf("767"), byAuthor.map { it.externalId })
        }

    @Test
    fun `title searches ignore author names and author searches ignore titles`() =
        runTest {
            addBook("1", "Austen Country", "Someone Else")
            addBook("2", "Emma", "Austen, Jane")

            val byTitle = bookFtsDao.findCandidates(matchQuery("austen", SearchScope.TITLE), limit = 10)
            val byAuthor = bookFtsDao.findCandidates(matchQuery("austen", SearchScope.AUTHOR), limit = 10)

            assertEquals(listOf("1"), byTitle.map { it.externalId }.distinct())
            assertEquals(listOf("2"), byAuthor.map { it.externalId }.distinct())
        }

    @Test
    fun `the indexed text lists are available for building the vocabulary`() =
        runTest {
            addBook("1", "Pride and Prejudice", "Austen, Jane")

            assertEquals(listOf("pride and prejudice"), bookFtsDao.allIndexedTitles())
            assertEquals(listOf("austen jane"), bookFtsDao.allIndexedAuthorNames())
        }
}
