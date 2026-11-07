package com.freelibrary.app.data.download

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.backup.BookStorage
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.BookFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class BookDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var server: MockWebServer
    private lateinit var booksDir: File
    private lateinit var downloader: BookDownloader
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
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
            server = MockWebServer().also { it.start() }
            booksDir = File(temporaryFolder.root, "books")
            downloader = downloaderWithLimit(1_000_000L)
        }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun downloaderWithLimit(limit: Long) =
        BookDownloader(
            OkHttpClient(),
            BookStorage(booksDir),
            database.downloadedBookDao(),
            TimeProvider { 5_000L },
            limit,
            allowHost = { true },
        )

    private fun url(path: String = "/book") = server.url(path).toString()

    private fun epubBytes(size: Int = 100) = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4) + ByteArray(size)

    private fun respond(
        body: ByteArray,
        code: Int = 200,
        type: String = "application/epub+zip",
    ) = server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", type).setBody(Buffer().write(body)))

    private fun filesLeft(): List<File> = booksDir.listFiles().orEmpty().toList()

    private suspend fun downloadOf(format: BookFileFormat) = downloader.download(bookId, format, url())

    private suspend fun assertFailed(
        expected: DownloadError,
        outcome: DownloadOutcome,
    ) {
        assertEquals(DownloadOutcome.Failed(expected), outcome)
        assertTrue("No file should be left: ${filesLeft()}", filesLeft().isEmpty())
        assertTrue(database.downloadedBookDao().getFormatsForBook(bookId).isEmpty())
    }

    @Test
    fun `an epub is saved under the book's id and recorded`() =
        runTest {
            val bytes = epubBytes()
            respond(bytes)

            val outcome = downloadOf(BookFileFormat.EPUB)

            val file = (outcome as DownloadOutcome.Done).file
            assertEquals(File(booksDir, "$bookId.epub").path, file.path)
            assertArrayEquals(bytes, file.readBytes())
            val rows = database.downloadedBookDao().getFormatsForBook(bookId)
            assertEquals(1, rows.size)
            assertEquals("epub", rows[0].format)
            assertEquals(file.path, rows[0].localFilePath)
            assertEquals(5_000L, rows[0].downloadedAt)
            assertEquals(listOf("$bookId.epub"), filesLeft().map { it.name })
        }

    @Test
    fun `a pdf and a text file are saved too`() =
        runTest {
            respond("%PDF-1.4 rest".toByteArray(), type = "application/pdf")
            respond("Hello reader".toByteArray(), type = "text/plain; charset=utf-8")

            val pdf = downloadOf(BookFileFormat.PDF)
            val text = downloadOf(BookFileFormat.TEXT)

            assertTrue(pdf is DownloadOutcome.Done)
            assertTrue(text is DownloadOutcome.Done)
            assertEquals(setOf("$bookId.pdf", "$bookId.txt"), filesLeft().map { it.name }.toSet())
            assertEquals(setOf("pdf", "txt"), database.downloadedBookDao().getFormatsForBook(bookId).map { it.format }.toSet())
        }

    @Test
    fun `progress is reported from nothing to the whole file`() =
        runTest {
            val bytes = epubBytes(200)
            respond(bytes)
            val seen = mutableListOf<DownloadProgress>()

            downloader.download(bookId, BookFileFormat.EPUB, url()) { seen += it }

            assertEquals(0L, seen.first().bytes)
            assertEquals(bytes.size.toLong(), seen.last().bytes)
            assertEquals(bytes.size.toLong(), seen.last().total)
        }

    @Test
    fun `a redirect is followed`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/real").toString()))
            respond(epubBytes())

            assertTrue(downloadOf(BookFileFormat.EPUB) is DownloadOutcome.Done)
        }

    @Test
    fun `a missing file is not found`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))

            assertFailed(DownloadError.NOT_FOUND, downloadOf(BookFileFormat.EPUB))
        }

    @Test
    fun `a refusal or a rate limit means blocked`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(403))
            assertFailed(DownloadError.BLOCKED, downloadOf(BookFileFormat.EPUB))

            server.enqueue(MockResponse().setResponseCode(429))
            assertFailed(DownloadError.BLOCKED, downloadOf(BookFileFormat.EPUB))
        }

    @Test
    fun `a server error is a server error`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))

            assertFailed(DownloadError.SERVER, downloadOf(BookFileFormat.EPUB))
        }

    @Test
    fun `a web page sent instead of an epub is not kept`() =
        runTest {
            respond("<html>Please wait</html>".toByteArray(), type = "text/html")

            assertFailed(DownloadError.NOT_A_BOOK, downloadOf(BookFileFormat.EPUB))
        }

    @Test
    fun `a file that does not start like its format is not kept`() =
        runTest {
            respond("this is not a zip".toByteArray())
            assertFailed(DownloadError.NOT_A_BOOK, downloadOf(BookFileFormat.EPUB))

            respond("this is not a pdf".toByteArray(), type = "application/pdf")
            assertFailed(DownloadError.NOT_A_BOOK, downloadOf(BookFileFormat.PDF))

            respond("  <!DOCTYPE html><html></html>".toByteArray(), type = "text/plain")
            assertFailed(DownloadError.NOT_A_BOOK, downloadOf(BookFileFormat.TEXT))
        }

    @Test
    fun `an empty answer is not a book`() =
        runTest {
            respond(ByteArray(0))

            assertFailed(DownloadError.NOT_A_BOOK, downloadOf(BookFileFormat.EPUB))
        }

    @Test
    fun `the next source is tried when one fails, in the order given`() =
        runTest {
            val bytes = epubBytes()
            respond(ByteArray(0), code = 404)
            respond(bytes)

            val outcome = downloader.downloadFirstThatWorks(bookId, BookFileFormat.EPUB, listOf(url("/a"), url("/b")))

            assertArrayEquals(bytes, (outcome as DownloadOutcome.Done).file.readBytes())
            assertEquals(listOf("/a", "/b"), listOf(server.takeRequest().path, server.takeRequest().path))
            assertEquals(listOf("$bookId.epub"), filesLeft().map { it.name })
        }

    @Test
    fun `the first source that works wins and the rest are not asked`() =
        runTest {
            respond(epubBytes())
            respond(epubBytes())

            val outcome = downloader.downloadFirstThatWorks(bookId, BookFileFormat.EPUB, listOf(url("/a"), url("/b")))

            assertTrue(outcome is DownloadOutcome.Done)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `when no source works the outcome is offline, or not found when every source lacks the file`() =
        runTest {
            respond(ByteArray(0), code = 500)
            respond(ByteArray(0), code = 403)
            assertFailed(
                DownloadError.OFFLINE,
                downloader.downloadFirstThatWorks(bookId, BookFileFormat.EPUB, listOf(url("/a"), url("/b"))),
            )

            respond(ByteArray(0), code = 404)
            respond(ByteArray(0), code = 404)
            assertFailed(
                DownloadError.NOT_FOUND,
                downloader.downloadFirstThatWorks(bookId, BookFileFormat.EPUB, listOf(url("/a"), url("/b"))),
            )
        }

    @Test
    fun `a single address reports its own error`() =
        runTest {
            respond(ByteArray(0), code = 403)

            assertFailed(DownloadError.BLOCKED, downloader.downloadFirstThatWorks(bookId, BookFileFormat.EPUB, listOf(url("/a"))))
        }

    @Test
    fun `a file that is too large stops the search`() =
        runTest {
            respond(epubBytes(2_000_000))
            respond(epubBytes())

            assertFailed(
                DownloadError.TOO_LARGE,
                downloader.downloadFirstThatWorks(bookId, BookFileFormat.EPUB, listOf(url("/a"), url("/b"))),
            )
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `a site that cannot be reached is passed over`() =
        runTest {
            val unreachable = MockWebServer().also { it.start() }
            val dead = unreachable.url("/a").toString()
            unreachable.shutdown()
            respond(epubBytes())

            val outcome = downloader.downloadFirstThatWorks(bookId, BookFileFormat.EPUB, listOf(dead, dead + "2", url("/b")))

            assertTrue(outcome is DownloadOutcome.Done)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `an unreachable site means offline`() =
        runTest {
            val address = url()
            server.shutdown()

            val outcome = downloader.download(bookId, BookFileFormat.EPUB, address)

            assertEquals(DownloadOutcome.Failed(DownloadError.OFFLINE), outcome)
            assertTrue(database.downloadedBookDao().getFormatsForBook(bookId).isEmpty())
        }

    @Test
    fun `a file bigger than the limit is refused, whether or not its size is announced`() =
        runTest {
            downloader = downloaderWithLimit(50L)
            respond(epubBytes(200))
            assertFailed(DownloadError.TOO_LARGE, downloadOf(BookFileFormat.EPUB))

            server.enqueue(
                MockResponse().setHeader("Content-Type", "application/epub+zip").setChunkedBody(Buffer().write(epubBytes(200)), 20),
            )
            assertFailed(DownloadError.TOO_LARGE, downloadOf(BookFileFormat.EPUB))
        }

    @Test
    fun `downloading again replaces the earlier copy and its record`() =
        runTest {
            respond(epubBytes(10))
            downloadOf(BookFileFormat.EPUB)
            val second = epubBytes(30)
            respond(second)

            val outcome = downloadOf(BookFileFormat.EPUB)

            assertArrayEquals(second, (outcome as DownloadOutcome.Done).file.readBytes())
            assertEquals(1, database.downloadedBookDao().getFormatsForBook(bookId).size)
            assertEquals(1, filesLeft().size)
        }

    @Test
    fun `a bad link is reported as not found`() =
        runTest {
            val outcome = downloader.download(bookId, BookFileFormat.EPUB, "not a link")

            assertEquals(DownloadOutcome.Failed(DownloadError.NOT_FOUND), outcome)
        }

    @Test
    fun `an address that is not an allowed one is refused before any request is made`() =
        runTest {
            val strict =
                BookDownloader(OkHttpClient(), BookStorage(booksDir), database.downloadedBookDao(), TimeProvider { 5_000L }, 1_000_000L)

            assertEquals(DownloadOutcome.Failed(DownloadError.NOT_FOUND), strict.download(bookId, BookFileFormat.EPUB, url()))
            assertEquals(
                DownloadOutcome.Failed(DownloadError.NOT_FOUND),
                strict.download(bookId, BookFileFormat.EPUB, "https://example.com/book.epub"),
            )
            assertEquals(0, server.requestCount)
            assertTrue(filesLeft().isEmpty())
        }

    @Test
    fun `a redirect to an address that is not allowed is never asked for`() =
        runTest {
            val onlyThisServer =
                BookDownloader(
                    OkHttpClient(),
                    BookStorage(booksDir),
                    database.downloadedBookDao(),
                    TimeProvider { 5_000L },
                    1_000_000L,
                    allowHost = { it.port == server.port },
                )
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://other.example:81/book"))

            val outcome = onlyThisServer.download(bookId, BookFileFormat.EPUB, url())

            assertEquals(DownloadOutcome.Failed(DownloadError.NOT_FOUND), outcome)
            assertEquals(1, server.requestCount)
            assertTrue(filesLeft().isEmpty())
            assertTrue(database.downloadedBookDao().getFormatsForBook(bookId).isEmpty())
        }

    @Test
    fun `a redirect to an allowed address is followed`() =
        runTest {
            val onlyThisServer =
                BookDownloader(
                    OkHttpClient(),
                    BookStorage(booksDir),
                    database.downloadedBookDao(),
                    TimeProvider { 5_000L },
                    1_000_000L,
                    allowHost = { it.port == server.port },
                )
            server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", "/moved"))
            respond(epubBytes())

            assertTrue(onlyThisServer.download(bookId, BookFileFormat.EPUB, url()) is DownloadOutcome.Done)
            assertEquals(listOf("/book", "/moved"), listOf(server.takeRequest().path, server.takeRequest().path))
        }

    @Test
    fun `cancelling a download leaves no file and no record`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/epub+zip")
                    .setBody(Buffer().write(epubBytes(4_000)))
                    .throttleBody(100, 100, TimeUnit.MILLISECONDS),
            )
            val job = launch(Dispatchers.IO) { downloadOf(BookFileFormat.EPUB) }
            delay(500)

            job.cancelAndJoin()

            assertTrue("No file should be left: ${filesLeft()}", filesLeft().isEmpty())
            assertTrue(database.downloadedBookDao().getFormatsForBook(bookId).isEmpty())
        }

    @Test
    fun `deleting removes the file and its record`() =
        runTest {
            respond(epubBytes())
            downloadOf(BookFileFormat.EPUB)

            downloader.delete(bookId, BookFileFormat.EPUB)

            assertTrue(filesLeft().isEmpty())
            assertTrue(database.downloadedBookDao().getFormatsForBook(bookId).isEmpty())
        }

    @Test
    fun `deleting never removes a file outside the books folder`() =
        runTest {
            val outside = temporaryFolder.newFile("outside.epub")
            database.downloadedBookDao().insert(
                DownloadedBook(bookId = bookId, localFilePath = outside.path, format = "epub", downloadedAt = 1L),
            )

            downloader.delete(bookId, BookFileFormat.EPUB)

            assertTrue(outside.exists())
            assertTrue(database.downloadedBookDao().getFormatsForBook(bookId).isEmpty())
        }

    private fun downloaderWithRetries(
        retries: Int,
        pauseMillis: Long = 0L,
    ) = BookDownloader(
        OkHttpClient(),
        BookStorage(booksDir),
        database.downloadedBookDao(),
        TimeProvider { 5_000L },
        1_000_000L,
        allowHost = { true },
        retryPauseMillis = pauseMillis,
        maxRetries = retries,
    )

    /** Cuts the first answer off half way; later answers come from [later], given the Range header the request carried. */
    private fun cutOffFirstThen(
        asked: MutableList<String?>,
        later: (range: String?) -> MockResponse,
    ) {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range")
                    val first = asked.isEmpty()
                    asked.add(range)
                    return if (first) {
                        MockResponse()
                            .setHeader("Content-Type", "application/epub+zip")
                            .setBody(Buffer().write(RESUME_BYTES))
                            .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
                    } else {
                        later(range)
                    }
                }
            }
    }

    @Test
    fun `a transfer that breaks carries on from the bytes already received`() =
        runTest {
            val asked = CopyOnWriteArrayList<String?>()
            cutOffFirstThen(asked) { range ->
                val start = range!!.removePrefix("bytes=").removeSuffix("-").toInt()
                MockResponse()
                    .setResponseCode(206)
                    .setHeader("Content-Type", "application/epub+zip")
                    .setHeader("Content-Range", "bytes $start-${RESUME_BYTES.size - 1}/${RESUME_BYTES.size}")
                    .setBody(Buffer().write(RESUME_BYTES, start, RESUME_BYTES.size - start))
            }

            val outcome = downloaderWithRetries(retries = 3).download(bookId, BookFileFormat.EPUB, url())

            assertArrayEquals(RESUME_BYTES, (outcome as DownloadOutcome.Done).file.readBytes())
            assertEquals(null, asked.first())
            assertTrue("The second request should ask for a range: $asked", asked[1]!!.startsWith("bytes="))
            assertEquals(listOf("$bookId.epub"), filesLeft().map { it.name })
        }

    @Test
    fun `a server that ignores the range sends the file again and it is read from the start`() =
        runTest {
            val asked = CopyOnWriteArrayList<String?>()
            cutOffFirstThen(asked) {
                MockResponse().setHeader("Content-Type", "application/epub+zip").setBody(Buffer().write(RESUME_BYTES))
            }

            val outcome = downloaderWithRetries(retries = 3).download(bookId, BookFileFormat.EPUB, url())

            assertArrayEquals(RESUME_BYTES, (outcome as DownloadOutcome.Done).file.readBytes())
        }

    @Test
    fun `a range the server cannot serve starts the file again`() =
        runTest {
            val asked = CopyOnWriteArrayList<String?>()
            cutOffFirstThen(asked) { range ->
                if (range != null && asked.size == 2) {
                    MockResponse().setResponseCode(416)
                } else {
                    MockResponse().setHeader("Content-Type", "application/epub+zip").setBody(Buffer().write(RESUME_BYTES))
                }
            }

            val outcome = downloaderWithRetries(retries = 3).download(bookId, BookFileFormat.EPUB, url())

            assertArrayEquals(RESUME_BYTES, (outcome as DownloadOutcome.Done).file.readBytes())
        }

    @Test
    fun `a connection that comes back lets the download finish`() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            respond(epubBytes())

            val outcome = downloaderWithRetries(retries = 5).download(bookId, BookFileFormat.EPUB, url())

            assertTrue(outcome is DownloadOutcome.Done)
        }

    @Test
    fun `when the retries run out the reader is told the connection is down and nothing is left behind`() =
        runTest {
            repeat(DISCONNECTS) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }

            assertFailed(DownloadError.OFFLINE, downloaderWithRetries(retries = 2).download(bookId, BookFileFormat.EPUB, url()))
            // The first try and two more, and the client may itself repeat a request once.
            assertTrue("Expected at least 3 requests, saw ${server.requestCount}", server.requestCount >= 3)
        }

    @Test
    fun `a transfer that keeps breaking ends offline with no half file`() =
        runTest {
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest) =
                        MockResponse()
                            .setHeader("Content-Type", "application/epub+zip")
                            .setBody(Buffer().write(RESUME_BYTES))
                            .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
                }

            assertFailed(DownloadError.OFFLINE, downloaderWithRetries(retries = 2).download(bookId, BookFileFormat.EPUB, url()))
        }

    @Test
    fun `each retry waits for the pause`() =
        runTest {
            repeat(DISCONNECTS) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }
            val started = System.nanoTime()

            downloaderWithRetries(retries = 2, pauseMillis = 300L).download(bookId, BookFileFormat.EPUB, url())

            val elapsedMillis = (System.nanoTime() - started) / 1_000_000
            assertTrue("Two retries should wait at least 600 ms, waited $elapsedMillis", elapsedMillis >= 600)
        }

    private companion object {
        /** Enough refused connections for every try, however often the client repeats a request itself. */
        const val DISCONNECTS = 30

        val RESUME_BYTES = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4) + ByteArray(300_000) { (it % 251).toByte() }
    }
}
