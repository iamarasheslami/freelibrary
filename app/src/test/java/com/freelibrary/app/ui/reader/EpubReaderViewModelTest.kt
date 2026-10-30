package com.freelibrary.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.ShelfState
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.reader.EpubBook
import com.freelibrary.app.data.reader.ReaderStyle
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EpubReaderViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var readerRepository: ReaderRepository
    private lateinit var readingState: ReadingStateRepository
    private val openedBooks = mutableListOf<EpubBook>()
    private val settings = SettingsRepository(InMemoryPreferences())
    private var bookId: Long = 0

    private val container =
        """
        <?xml version="1.0"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
        </container>
        """.trimIndent()

    private val packageXml =
        """
        <?xml version="1.0"?>
        <package xmlns="http://www.idpf.org/2007/opf">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>The Book's Own Title</dc:title></metadata>
          <manifest>
            <item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
            <item id="c2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
          </manifest>
          <spine><itemref idref="c1"/><itemref idref="c2"/></spine>
        </package>
        """.trimIndent()

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
        // A save still on its way must finish while the main dispatcher is still set, or it fails in a later test.
        runBlocking { withTimeoutOrNull(5_000) { created.forEach { it.viewModelScope.coroutineContext[Job]?.cancelAndJoin() } } }
        openedBooks.forEach { it.close() }
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun downloadEpub() {
        val file = temporaryFolder.newFile("$bookId.epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            val entries =
                listOf(
                    "META-INF/container.xml" to container,
                    "OEBPS/content.opf" to packageXml,
                    "OEBPS/ch1.xhtml" to "<html/>",
                    "OEBPS/ch2.xhtml" to "<html/>",
                )
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        register(file)
    }

    private suspend fun register(file: File) {
        database.downloadedBookDao().insert(DownloadedBook(bookId = bookId, localFilePath = file.path, format = "epub", downloadedAt = 1L))
    }

    private val created = mutableListOf<EpubReaderViewModel>()

    private fun viewModel(arguments: Map<String, Any?> = mapOf(ARG_BOOK_ID to bookId)): EpubReaderViewModel =
        build(arguments).also { created.add(it) }

    private fun build(arguments: Map<String, Any?>) =
        EpubReaderViewModel(
            SavedStateHandle(arguments),
            readerRepository,
            readingState,
            BookDetailsRepository(database.bookDao(), database.bookAuthorDao(), database.bookSummaryDao()),
            settings,
        )

    private suspend fun EpubReaderViewModel.settled(): EpubReaderUiState {
        val state = withTimeout(10_000) { uiState.first { it !is EpubReaderUiState.Loading } }
        if (state is EpubReaderUiState.Ready) openedBooks.add(state.book)
        return state
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
    fun `the text size steps within its limits and the size is kept`() =
        runBlocking<Unit> {
            downloadEpub()
            val first = viewModel()
            first.settled()
            assertEquals(ReaderStyle.DEFAULT_TEXT_PERCENT, first.textPercent.value)

            first.changeTextSize(larger = true)
            assertEquals(110, first.textPercent.value)
            first.changeTextSize(larger = false)
            first.changeTextSize(larger = false)
            assertEquals(90, first.textPercent.value)

            repeat(40) { first.changeTextSize(larger = false) }
            assertEquals(ReaderStyle.MIN_TEXT_PERCENT, first.textPercent.value)
            repeat(40) { first.changeTextSize(larger = true) }
            assertEquals(ReaderStyle.MAX_TEXT_PERCENT, first.textPercent.value)

            eventually { settings.readerTextPercent.first().takeIf { it == ReaderStyle.MAX_TEXT_PERCENT } }
            val second = viewModel()
            second.settled()
            assertEquals(ReaderStyle.MAX_TEXT_PERCENT, second.textPercent.value)
        }

    @Test
    fun `a size picked on the text size bar is kept within the limits`() =
        runBlocking<Unit> {
            downloadEpub()
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
    fun `the book is shown page by page until the reader chooses scrolling, and the choice is kept`() =
        runBlocking<Unit> {
            downloadEpub()
            val first = viewModel()
            first.settled()
            assertTrue(first.paged.value)

            first.setPaged(false)

            assertFalse(first.paged.value)
            eventually { settings.readerPaged.first().takeIf { !it } }
            val second = viewModel()
            second.settled()
            assertFalse(second.paged.value)
        }

    @Test
    fun `without a book in the arguments there is nothing to open`() =
        runBlocking<Unit> {
            val state = viewModel(emptyMap()).settled()

            assertEquals(EpubReaderUiState.Failed(ReaderFailure.NOT_DOWNLOADED), state)
        }

    @Test
    fun `a book that was not downloaded cannot be opened`() =
        runBlocking<Unit> {
            val state = viewModel().settled()

            assertEquals(EpubReaderUiState.Failed(ReaderFailure.NOT_DOWNLOADED), state)
        }

    @Test
    fun `a downloaded EPUB opens at the start with the catalog title`() =
        runBlocking<Unit> {
            downloadEpub()
            val viewModel = viewModel()

            val state = viewModel.settled() as EpubReaderUiState.Ready

            assertEquals("Moby Dick", state.title)
            assertEquals(2, state.book.chapters.size)
            assertEquals(ReaderPosition.Epub(0, 0), viewModel.position)
        }

    @Test
    fun `opening the book makes it Currently Reading`() =
        runBlocking<Unit> {
            downloadEpub()

            viewModel().settled()

            assertEquals(ShelfState.READING, database.bookShelfStateDao().getForBook(bookId)?.state)
        }

    @Test
    fun `opening a finished book leaves it finished`() =
        runBlocking<Unit> {
            downloadEpub()
            readingState.markFinished(bookId)

            viewModel().settled()

            assertEquals(ShelfState.FINISHED, database.bookShelfStateDao().getForBook(bookId)?.state)
        }

    @Test
    fun `the book opens where the reader stopped`() =
        runBlocking<Unit> {
            downloadEpub()
            readerRepository.savePosition(bookId, ReaderPosition.Epub(1, 400))
            val viewModel = viewModel()

            viewModel.settled()

            assertEquals(ReaderPosition.Epub(1, 400), viewModel.position)
        }

    @Test
    fun `a saved chapter the book no longer has opens at the last chapter's start`() =
        runBlocking<Unit> {
            downloadEpub()
            readerRepository.savePosition(bookId, ReaderPosition.Epub(9, 400))
            val viewModel = viewModel()

            viewModel.settled()

            assertEquals(ReaderPosition.Epub(1, 0), viewModel.position)
        }

    @Test
    fun `a place asked for opens the book there instead of where the reader stopped`() =
        runBlocking<Unit> {
            downloadEpub()
            readerRepository.savePosition(bookId, ReaderPosition.Epub(0, 100))
            val viewModel = viewModel(mapOf(ARG_BOOK_ID to bookId, ARG_START_POSITION to "epub:1:250"))

            viewModel.settled()

            assertEquals(ReaderPosition.Epub(1, 250), viewModel.position)
        }

    @Test
    fun `a place of another kind of book is ignored`() =
        runBlocking<Unit> {
            downloadEpub()
            readerRepository.savePosition(bookId, ReaderPosition.Epub(1, 400))
            val viewModel = viewModel(mapOf(ARG_BOOK_ID to bookId, ARG_START_POSITION to "pdf:3"))

            viewModel.settled()

            assertEquals(ReaderPosition.Epub(1, 400), viewModel.position)
        }

    @Test
    fun `a file that is not an EPUB cannot be opened`() =
        runBlocking<Unit> {
            val file = temporaryFolder.newFile("$bookId.epub")
            file.writeText("This is not an EPUB.")
            register(file)

            val state = viewModel().settled()

            assertEquals(EpubReaderUiState.Failed(ReaderFailure.NOT_AN_EPUB), state)
        }

    @Test
    fun `a position the reader moves to is saved by saveNow`() =
        runBlocking<Unit> {
            downloadEpub()
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onPositionChanged(1, 700)
            viewModel.saveNow()

            val saved = eventually { readerRepository.savedEpubPosition(bookId)?.takeIf { it == ReaderPosition.Epub(1, 700) } }
            assertEquals(ReaderPosition.Epub(1, 700), saved)
        }

    @Test
    fun `a chapter the book does not have is ignored`() =
        runBlocking<Unit> {
            downloadEpub()
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onPositionChanged(5, 10)
            viewModel.onPositionChanged(-1, 10)

            assertEquals(ReaderPosition.Epub(0, 0), viewModel.position)
        }

    @Test
    fun `reaching the end of the last chapter asks once`() =
        runBlocking<Unit> {
            downloadEpub()
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onPositionChanged(1, 999)
            assertNotNull(withTimeoutOrNull(5_000) { viewModel.endReached.first() })

            viewModel.onPositionChanged(1, 1000)
            assertNull(withTimeoutOrNull(500) { viewModel.endReached.first() })
        }

    @Test
    fun `the end is not announced before the last chapter`() =
        runBlocking<Unit> {
            downloadEpub()
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onPositionChanged(0, 1000)

            assertNull(withTimeoutOrNull(500) { viewModel.endReached.first() })
        }

    @Test
    fun `a book that is already finished does not ask`() =
        runBlocking<Unit> {
            downloadEpub()
            readingState.markFinished(bookId)
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onPositionChanged(1, 999)

            assertNull(withTimeoutOrNull(500) { viewModel.endReached.first() })
        }

    @Test
    fun `marking the book finished from the prompt moves it to Already Read`() =
        runBlocking<Unit> {
            downloadEpub()
            val viewModel = viewModel()
            viewModel.settled()

            viewModel.onMarkFinished()

            val state = eventually { database.bookShelfStateDao().getForBook(bookId)?.state?.takeIf { it == ShelfState.FINISHED } }
            assertEquals(ShelfState.FINISHED, state)
        }
}
