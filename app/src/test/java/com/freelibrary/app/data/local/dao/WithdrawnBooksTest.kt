package com.freelibrary.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookFts
import com.freelibrary.app.data.local.entity.BookShelfState
import com.freelibrary.app.data.local.entity.Bookshelf
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.local.entity.WithdrawnBook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A withdrawn book the reader still has stays in the database but leaves browsing and search, never the reader's own lists. */
@RunWith(RobolectricTestRunner::class)
class WithdrawnBooksTest {
    private lateinit var database: FreeLibraryDatabase
    private var keptId = 0L
    private var withdrawnId = 0L

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            val shelfId = database.bookshelfDao().insert(Bookshelf(name = "Poetry"))

            suspend fun add(
                externalId: String,
                title: String,
            ): Long {
                val id =
                    database.bookDao().insert(
                        Book(
                            sourceId = sourceId,
                            externalId = externalId,
                            title = title,
                            issuedDate = "2026-01-01",
                            primaryLanguage = "en",
                            locc = null,
                            lastModified = "2026-10-04",
                        ),
                    )
                database.bookBookshelfDao().insert(BookBookshelf(bookId = id, bookshelfId = shelfId))
                database.bookFtsDao().upsert(BookFts(bookId = id, title = title.lowercase(), authorNames = ""))
                return id
            }
            keptId = add("1", "kept poems")
            withdrawnId = add("2", "withdrawn poems")
            database.bookShelfStateDao().upsert(BookShelfState(withdrawnId, ShelfState.READING, 5L))
            database.withdrawnBookDao().mark(WithdrawnBook(withdrawnId))
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `browsing, counts, options and search leave a withdrawn book out`() =
        runTest {
            val list = database.bookListDao()
            val shelf = list.observeBooksOnShelf("Poetry", "en", 10, emptyList(), true, emptyList(), true).first()
            assertEquals(listOf(keptId), shelf.map { it.bookId })
            assertEquals(listOf(keptId), list.observeRecentlyAdded("en", 10).first().map { it.bookId })
            assertEquals(1, list.shelfBookCounts("en", 1, emptyList(), true, emptyList(), true).single().bookCount)
            assertEquals(1, list.languageOptions().single().bookCount)
            assertEquals(listOf(keptId), database.bookFtsDao().search("poems").map { it.id })
        }

    @Test
    fun `the reader's own lists still show a withdrawn book`() =
        runTest {
            val reading = database.bookListDao().observeBooksInState(ShelfState.READING, 10).first()
            assertEquals(listOf(withdrawnId), reading.map { it.bookId })
        }

    @Test
    fun `reader data is detected, and a book with none is not`() =
        runTest {
            assertTrue(database.withdrawnBookDao().hasReaderData(withdrawnId))
            assertFalse(database.withdrawnBookDao().hasReaderData(keptId))
        }

    @Test
    fun `unmarking a book brings it back`() =
        runTest {
            database.withdrawnBookDao().unmark(withdrawnId)
            val shelf = database.bookListDao().observeRecentlyAdded("en", 10).first()
            assertEquals(2, shelf.size)
        }
}
