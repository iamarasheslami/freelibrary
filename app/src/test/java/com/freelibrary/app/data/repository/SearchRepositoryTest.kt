package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Author
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookAuthor
import com.freelibrary.app.data.local.entity.BookFts
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.shared.SearchScope
import com.freelibrary.shared.SearchText
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: SearchRepository
    private var sourceId: Long = 0

    private suspend fun addBook(
        externalId: String,
        title: String,
        vararg authorNames: String,
    ) {
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
        database.bookFtsDao().upsert(
            BookFts(
                bookId = bookId,
                title = SearchText.fold(title),
                authorNames = SearchText.fold(authorNames.joinToString(" ")),
            ),
        )
    }

    private suspend fun titles(
        query: String,
        scope: SearchScope = SearchScope.TITLE,
    ) = repository.search(query, scope).map { it.title }

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            repository = SearchRepository(database.bookFtsDao())
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `an exact search puts the exact title first`() =
        runTest {
            addBook("1", "Some Remarks on the Tragedy of Hamlet", "Critic, A.")
            addBook("2", "Hamlet", "Shakespeare, William")

            assertEquals(listOf("Hamlet", "Some Remarks on the Tragedy of Hamlet"), titles("hamlet"))
        }

    @Test
    fun `a misspelled title is still found`() =
        runTest {
            addBook("1342", "Pride and Prejudice", "Austen, Jane")

            assertEquals(listOf("Pride and Prejudice"), titles("pride and prejudise"))
        }

    @Test
    fun `a real word that is one edit from the intended one still reaches it`() =
        runTest {
            addBook("1", "The Adventures of Sherlock Holmes", "Doyle, Arthur Conan")
            addBook("2", "Homes of the Poor", "Reformer, A.")
            addBook("3", "Home Economics", "Teacher, B.")

            assertEquals(listOf("The Adventures of Sherlock Holmes"), titles("sherlock homes"))
        }

    @Test
    fun `a misspelled author is still found`() =
        runTest {
            addBook("730", "Oliver Twist", "Dickens, Charles")

            assertEquals(listOf("Oliver Twist"), titles("charles dikens", SearchScope.AUTHOR))
        }

    @Test
    fun `a wrong first letter is fixed by the widened pass`() =
        runTest {
            addBook("1342", "Pride and Prejudice", "Austen, Jane")

            assertEquals(listOf("Pride and Prejudice"), titles("bride and prejudice"))
        }

    @Test
    fun `an elision typed without its apostrophe still finds the book`() =
        runTest {
            addBook("1", "Mémoires de Mr. d'Artagnan", "Courtilz de Sandras, Gatien")

            assertEquals(listOf("Mémoires de Mr. d'Artagnan"), titles("dartagnan"))
        }

    @Test
    fun `a query with no searchable words gives no results`() =
        runTest {
            addBook("1", "Hamlet", "Shakespeare, William")

            assertTrue(repository.search("  !!! ", SearchScope.TITLE).isEmpty())
        }

    @Test
    fun `the primary author ranks above a co-author`() =
        runTest {
            addBook("1", "An Anthology", "Gogol, Nikolai", "Dostoyevsky, Fyodor")
            addBook("2", "Poor Folk", "Dostoyevsky, Fyodor")

            assertEquals(listOf("Poor Folk", "An Anthology"), titles("dostoyevsky", SearchScope.AUTHOR))
        }

    @Test
    fun `results carry the authors in credit order`() =
        runTest {
            addBook("1", "The Gambler", "Dostoyevsky, Fyodor", "Hogarth, C. J.")

            val result = repository.search("gambler", SearchScope.TITLE).single()

            assertEquals(listOf("Dostoyevsky, Fyodor", "Hogarth, C. J."), result.authors)
            assertEquals("1", result.externalId)
        }

    @Test
    fun `the limit caps the number of results`() =
        runTest {
            (1..5).forEach { addBook("$it", "Tales $it", "Author $it") }

            assertEquals(2, repository.search("tales", SearchScope.TITLE, limit = 2).size)
        }

    @Test
    fun `new words are only corrected after the vocabulary is invalidated`() =
        runTest {
            addBook("1", "Alpha Book", "Writer, A.")
            assertEquals(listOf("Alpha Book"), titles("alpah"))

            addBook("2", "Zeppelin Flight", "Pilot, B.")
            assertTrue(titles("zepelin").isEmpty())

            repository.invalidateVocabularies()
            assertEquals(listOf("Zeppelin Flight"), titles("zepelin"))
        }
}
