package com.freelibrary.app.data.download

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.backup.BookStorage
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.BookFileFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class BookDownloadManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var server: MockWebServer
    private lateinit var booksDir: File
    private lateinit var manager: BookDownloadManager
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runBlocking<Unit> {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            server = MockWebServer().also { it.start() }
            booksDir = File(temporaryFolder.root, "books")
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
                        lastModified = "2026-10-16",
                    ),
                )
            database.bookFormatDao().insertAll(
                listOf(
                    BookFormat(bookId = bookId, formatType = "txt", downloadUrl = server.url("/book.txt").toString()),
                    BookFormat(bookId = bookId, formatType = "epub", downloadUrl = server.url("/book.epub").toString()),
                    BookFormat(bookId = bookId, formatType = "pdf", downloadUrl = server.url("/book.pdf").toString()),
                ),
            )
            val downloader =
                BookDownloader(OkHttpClient(), BookStorage(booksDir), database.downloadedBookDao(), TimeProvider { 5_000L }, 1_000_000L)
            manager = BookDownloadManager(downloader, database.bookFormatDao(), database.downloadedBookDao())
        }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun epubBytes(size: Int = 100) = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4) + ByteArray(size)

    private fun respondWithEpub() =
        server.enqueue(MockResponse().setHeader("Content-Type", "application/epub+zip").setBody(Buffer().write(epubBytes())))

    private fun respondSlowly() =
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/epub+zip")
                .setBody(Buffer().write(epubBytes(4_000)))
                .throttleBody(100, 100, TimeUnit.MILLISECONDS),
        )

    private suspend fun awaitFiles(condition: (BookFiles) -> Boolean): BookFiles =
        withTimeout(10_000) { manager.observe(bookId).first(condition) }

    @Test
    fun `a book's files list the formats the catalog offers, EPUB first`() =
        runBlocking<Unit> {
            val files = awaitFiles { true }

            assertEquals(listOf(BookFileFormat.EPUB, BookFileFormat.PDF, BookFileFormat.TEXT), files.available)
            assertEquals(emptyList<BookFileFormat>(), files.downloaded)
            assertNull(files.transfer)
            assertEquals(listOf(BookFileFormat.EPUB, BookFileFormat.PDF, BookFileFormat.TEXT), files.toDownload)
        }

    @Test
    fun `a started download ends with the file downloaded and nothing left running`() =
        runBlocking<Unit> {
            respondWithEpub()

            manager.start(bookId, BookFileFormat.EPUB)

            val files = awaitFiles { BookFileFormat.EPUB in it.downloaded && it.transfer == null }
            assertEquals(listOf(BookFileFormat.PDF, BookFileFormat.TEXT), files.toDownload)
            assertTrue(File(booksDir, "$bookId.epub").isFile)
        }

    @Test
    fun `a failed download is remembered until it is tried again`() =
        runBlocking<Unit> {
            server.enqueue(MockResponse().setResponseCode(404))
            manager.start(bookId, BookFileFormat.EPUB)

            val failed = awaitFiles { it.transfer is Transfer.Failed }
            assertEquals(Transfer.Failed(BookFileFormat.EPUB, DownloadError.NOT_FOUND), failed.transfer)
            assertEquals(false, failed.busy)

            respondWithEpub()
            manager.start(bookId, BookFileFormat.EPUB)
            awaitFiles { BookFileFormat.EPUB in it.downloaded && it.transfer == null }
        }

    @Test
    fun `a retry made the moment a failure is shown is always accepted`() =
        runBlocking<Unit> {
            repeat(25) {
                server.enqueue(MockResponse().setResponseCode(404))
                manager.start(bookId, BookFileFormat.EPUB)
                awaitFiles { it.transfer is Transfer.Failed }

                respondWithEpub()
                manager.start(bookId, BookFileFormat.EPUB)
                awaitFiles { BookFileFormat.EPUB in it.downloaded && it.transfer == null }

                manager.delete(bookId, BookFileFormat.EPUB)
                awaitFiles { it.downloaded.isEmpty() }
            }
        }

    @Test
    fun `a format the catalog does not list fails as not found`() =
        runBlocking<Unit> {
            database.bookFormatDao().deleteForBook(bookId)

            manager.start(bookId, BookFileFormat.EPUB)

            val failed = awaitFiles { it.transfer is Transfer.Failed }
            assertEquals(Transfer.Failed(BookFileFormat.EPUB, DownloadError.NOT_FOUND), failed.transfer)
        }

    @Test
    fun `cancelling stops the download and leaves no file or record`() =
        runBlocking<Unit> {
            respondSlowly()
            manager.start(bookId, BookFileFormat.EPUB)
            awaitFiles { it.transfer is Transfer.Running }

            manager.cancel(bookId)

            val files = awaitFiles { it.transfer == null }
            assertEquals(emptyList<BookFileFormat>(), files.downloaded)
            withTimeout(5_000) {
                while (booksDir.listFiles().orEmpty().isNotEmpty()) delay(50)
            }
        }

    @Test
    fun `asking again while a book is downloading does not start a second download`() =
        runBlocking<Unit> {
            respondSlowly()
            manager.start(bookId, BookFileFormat.EPUB)
            awaitFiles { it.transfer is Transfer.Running }

            manager.start(bookId, BookFileFormat.EPUB)
            manager.start(bookId, BookFileFormat.PDF)
            delay(300)

            assertEquals(1, server.requestCount)
            assertTrue(awaitFiles { true }.busy)
            manager.cancel(bookId)
        }

    @Test
    fun `deleting a download removes it from the book's files`() =
        runBlocking<Unit> {
            respondWithEpub()
            manager.start(bookId, BookFileFormat.EPUB)
            awaitFiles { BookFileFormat.EPUB in it.downloaded && it.transfer == null }

            manager.delete(bookId, BookFileFormat.EPUB)

            awaitFiles { it.downloaded.isEmpty() }
            assertTrue(booksDir.listFiles().orEmpty().isEmpty())
        }
}
