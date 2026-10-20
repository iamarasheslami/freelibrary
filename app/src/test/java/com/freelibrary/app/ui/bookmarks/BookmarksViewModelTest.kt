package com.freelibrary.app.ui.bookmarks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.BookmarkRepository
import com.freelibrary.app.data.repository.ReaderRepository
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookmarksViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var bookmarks: BookmarkRepository
    private lateinit var readerRepository: ReaderRepository
    private val created = mutableListOf<BookmarksViewModel>()
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            bookmarks = BookmarkRepository(database.bookmarkDao(), TimeProvider { 5L })
            readerRepository = ReaderRepository(database.downloadedBookDao(), database.readingProgressDao(), TimeProvider { 0L })
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            bookId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "2701",
                        title = "Moby Dick",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-10-16",
                    ),
                )
        }

    @After
    fun tearDown() {
        // Work still on its way must finish while the main dispatcher is still set, or it fails in a later test.
        runBlocking { withTimeoutOrNull(5_000) { created.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } } }
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun downloadText(length: Int) {
        val file = temporaryFolder.newFile("$bookId.txt")
        file.writeText("a".repeat(length))
        database.downloadedBookDao().insert(DownloadedBook(bookId = bookId, localFilePath = file.path, format = "txt", downloadedAt = 1L))
    }

    private fun viewModel(): BookmarksViewModel =
        BookmarksViewModel(
            SavedStateHandle(mapOf(ARG_BOOK_ID to bookId)),
            bookmarks,
            readerRepository,
            BookDetailsRepository(database.bookDao(), database.bookAuthorDao(), database.bookSummaryDao()),
        ).also { created.add(it) }

    private suspend fun <T : Any> eventually(block: suspend () -> T?): T =
        withTimeout(10_000) {
            var value = block()
            while (value == null) {
                delay(20)
                value = block()
            }
            value
        }

    @Test
    fun `a book without bookmarks gives an empty list`() =
        runBlocking<Unit> {
            val viewModel = viewModel()

            assertEquals(emptyList<BookmarkItem>(), eventually { viewModel.items.value })
        }

    @Test
    fun `the book title and the percent through a text book are known`() =
        runBlocking<Unit> {
            downloadText(10_000)
            bookmarks.add(bookId, ReaderPosition.Text(2_500), "Quote")
            bookmarks.add(bookId, ReaderPosition.Epub(1, 400), null)
            val viewModel = viewModel()

            val items = eventually { viewModel.items.value?.takeIf { it.size == 2 && it.any { item -> item.textPercent != null } } }

            assertEquals("Moby Dick", eventually { viewModel.bookTitle.value })
            assertNull(items.first { it.bookmark.position is ReaderPosition.Epub }.textPercent)
            assertEquals(25, items.first { it.bookmark.position is ReaderPosition.Text }.textPercent)
        }

    @Test
    fun `a bookmark opens only while the file of its format is downloaded`() =
        runBlocking<Unit> {
            val viewModel = viewModel()
            assertFalse(viewModel.canOpen(ReaderPosition.Text(1)))

            downloadText(100)

            assertTrue(viewModel.canOpen(ReaderPosition.Text(1)))
            assertFalse(viewModel.canOpen(ReaderPosition.Epub(0, 0)))
            assertFalse(viewModel.canOpen(ReaderPosition.Pdf(0)))
        }

    @Test
    fun `a deleted bookmark can be undone`() =
        runBlocking<Unit> {
            bookmarks.add(bookId, ReaderPosition.Pdf(3), "Chart")
            val viewModel = viewModel()
            val bookmark = eventually { viewModel.items.value?.singleOrNull() }.bookmark

            viewModel.delete(bookmark)
            assertEquals(emptyList<BookmarkItem>(), eventually { viewModel.items.value?.takeIf { it.isEmpty() } })

            viewModel.restore(bookmark)
            assertEquals(bookmark, eventually { viewModel.items.value?.singleOrNull() }.bookmark)
        }
}
