package com.freelibrary.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.reader.ReaderStyle
import com.freelibrary.app.data.reader.TextBook
import com.freelibrary.app.data.repository.BookDetailsRepository
import com.freelibrary.app.data.repository.InMemoryPreferences
import com.freelibrary.app.data.repository.ReaderRepository
import com.freelibrary.app.data.repository.ReadingStateRepository
import com.freelibrary.app.data.repository.SettingsRepository
import com.freelibrary.app.domain.model.ReaderPosition
import com.freelibrary.app.ui.bookpage.ARG_BOOK_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
class TextReaderViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var readerRepository: ReaderRepository
    private lateinit var readingState: ReadingStateRepository
    private val settings = SettingsRepository(InMemoryPreferences())
    private val created = mutableListOf<TextReaderViewModel>()
    private var bookId: Long = 0

    private val text = (1..300).joinToString("\n\n") { "Paragraph $it " + "word ".repeat(100) }

    @Before
    fun setUp() =
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            readingState =
                ReadingStateRepository(database, database.bookShelfStateDao(), database.bookListDao(), TimeProvider { 0L })
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

    private suspend fun downloadText(content: String) {
        val file = temporaryFolder.newFile("$bookId.txt")
        file.writeText(content)
        database.downloadedBookDao().insert(DownloadedBook(bookId = bookId, localFilePath = file.path, format = "txt", downloadedAt = 1L))
    }

    private fun viewModel(arguments: Map<String, Any?> = mapOf(ARG_BOOK_ID to bookId)): TextReaderViewModel =
        build(arguments).also { created.add(it) }

    private fun build(arguments: Map<String, Any?>) =
        TextReaderViewModel(
            SavedStateHandle(arguments),
            readerRepository,
            readingState,
            BookDetailsRepository(database.bookDao(), database.bookAuthorDao(), database.bookSummaryDao()),
            settings,
        )

    private suspend fun TextReaderViewModel.settled(): TextReaderUiState =
        withTimeout(10_000) {
            uiState.first { it !is TextReaderUiState.Loading }
        }

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
    fun `a book that was not downloaded as text cannot be opened`() =
        runBlocking<Unit> {
            assertEquals(TextReaderUiState.Failed(TextFailure.NOT_DOWNLOADED), viewModel().settled())
            assertEquals(TextReaderUiState.Failed(TextFailure.NOT_DOWNLOADED), viewModel(emptyMap()).settled())
        }

    @Test
    fun `a file with nothing in it is reported`() =
        runBlocking<Unit> {
            downloadText("   \n\n  ")

            assertEquals(TextReaderUiState.Failed(TextFailure.EMPTY), viewModel().settled())
        }

    @Test
    fun `a text book opens at its start`() =
        runBlocking<Unit> {
            downloadText(text)
            val viewModel = viewModel()

            val state = viewModel.settled()

            assertTrue(state is TextReaderUiState.Ready)
            assertEquals("Moby Dick", (state as TextReaderUiState.Ready).title)
            assertEquals(TextPlace(0, 0), viewModel.position)
        }

    @Test
    fun `the reader resumes where it left the book`() =
        runBlocking<Unit> {
            downloadText(text)
            val reference = TextBook.fromText(text)!!
            val offset = 100_000
            readerRepository.savePosition(bookId, ReaderPosition.Text(offset))
            val viewModel = viewModel()
            viewModel.settled()

            val (part, permille) = reference.locate(offset)

            assertEquals(TextPlace(part, permille), viewModel.position)
            assertTrue(part > 0)
        }

    @Test
    fun `a place asked for opens the book there instead of where the reader stopped`() =
        runBlocking<Unit> {
            downloadText(text)
            val reference = TextBook.fromText(text)!!
            readerRepository.savePosition(bookId, ReaderPosition.Text(10))
            val viewModel = viewModel(mapOf(ARG_BOOK_ID to bookId, ARG_START_POSITION to "text:100000"))

            viewModel.settled()

            val (part, permille) = reference.locate(100_000)
            assertEquals(TextPlace(part, permille), viewModel.position)
        }

    @Test
    fun `a place of another kind of book is ignored`() =
        runBlocking<Unit> {
            downloadText(text)
            val reference = TextBook.fromText(text)!!
            readerRepository.savePosition(bookId, ReaderPosition.Text(100_000))
            val viewModel = viewModel(mapOf(ARG_BOOK_ID to bookId, ARG_START_POSITION to "epub:1:250"))

            viewModel.settled()

            val (part, permille) = reference.locate(100_000)
            assertEquals(TextPlace(part, permille), viewModel.position)
        }

    @Test
    fun `the place is saved as a number of characters`() =
        runBlocking<Unit> {
            downloadText(text)
            val reference = TextBook.fromText(text)!!
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onPositionChanged(2, 500)
            viewModel.saveNow()

            val expected = ReaderPosition.Text(reference.offsetOf(2, 500))
            assertEquals(expected, eventually { readerRepository.savedTextPosition(bookId)?.takeIf { it == expected } })
        }

    @Test
    fun `reaching the end of the last part asks once whether to mark the book finished`() =
        runBlocking<Unit> {
            downloadText(text)
            val viewModel = viewModel()
            val book = (viewModel.settled() as TextReaderUiState.Ready).book

            viewModel.onPositionChanged(book.parts.lastIndex, 990)
            withTimeout(10_000) { viewModel.endReached.first() }

            viewModel.onPositionChanged(book.parts.lastIndex, 995)
            assertNull(withTimeoutOrNull(300) { viewModel.endReached.first() })
        }

    @Test
    fun `the middle of the book does not ask to mark it finished`() =
        runBlocking<Unit> {
            downloadText(text)
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onPositionChanged(0, 990)

            assertNull(withTimeoutOrNull(300) { viewModel.endReached.first() })
        }

    @Test
    fun `a size picked on the text size bar is kept within the limits`() =
        runBlocking<Unit> {
            downloadText(text)
            val first = viewModel()
            first.settled()

            first.setTextSize(150)
            assertEquals(150, first.textPercent.value)
            first.setTextSize(10)
            assertEquals(ReaderStyle.MIN_TEXT_PERCENT, first.textPercent.value)
            first.setTextSize(999)
            assertEquals(ReaderStyle.MAX_TEXT_PERCENT, first.textPercent.value)

            val kept = eventually { settings.readerTextPercent.first().takeIf { it == ReaderStyle.MAX_TEXT_PERCENT } }
            assertEquals(ReaderStyle.MAX_TEXT_PERCENT, kept)
        }

    @Test
    fun `the text size steps within its limits and the view choice is kept`() =
        runBlocking<Unit> {
            downloadText(text)
            val first = viewModel()
            first.settled()
            assertEquals(ReaderStyle.DEFAULT_TEXT_PERCENT, first.textPercent.value)
            assertTrue(first.paged.value)

            first.changeTextSize(larger = true)
            assertEquals(110, first.textPercent.value)
            repeat(40) { first.changeTextSize(larger = false) }
            assertEquals(ReaderStyle.MIN_TEXT_PERCENT, first.textPercent.value)

            first.setPaged(false)
            assertFalse(first.paged.value)

            eventually { settings.readerPaged(bookId).first().takeIf { !it } }
            eventually { settings.readerTextPercent.first().takeIf { it == ReaderStyle.MIN_TEXT_PERCENT } }
            val second = viewModel()
            second.settled()
            assertFalse(second.paged.value)
            assertEquals(ReaderStyle.MIN_TEXT_PERCENT, second.textPercent.value)
        }
}
