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
import com.freelibrary.app.ui.settings.StorageState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DownloadsStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var booksDir: File
    private lateinit var storage: DownloadsStorage
    private var sourceId = 0L

    @Before
    fun setUp() =
        runTest {
            database =
                Room.inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    FreeLibraryDatabase::class.java,
                ).allowMainThreadQueries().build()
            sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            booksDir = File(temporaryFolder.root, "books").also { it.mkdirs() }
            val downloader =
                BookDownloader(OkHttpClient(), BookStorage(booksDir), database.downloadedBookDao(), TimeProvider { 5_000L }, 1_000_000L)
            storage = DownloadsStorage(database.downloadedBookDao(), downloader)
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun download(
        id: String,
        title: String,
        format: String,
        bytes: Int,
        at: Long,
    ): File {
        val bookId =
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = id,
                    title = title,
                    issuedDate = null,
                    primaryLanguage = "en",
                    locc = null,
                    lastModified = "2026-10-16",
                ),
            )
        val file = File(booksDir, "$id.$format").also { it.writeBytes(ByteArray(bytes)) }
        database.downloadedBookDao().insert(
            DownloadedBook(bookId = bookId, localFilePath = file.path, format = format, downloadedAt = at),
        )
        return file
    }

    @Test
    fun `lists the downloads newest first with titles and sizes`() =
        runTest {
            download("1", "Old", "epub", 100, at = 1L)
            download("2", "New", "txt", 250, at = 2L)

            val list = storage.observe().first()

            assertEquals(listOf("New", "Old"), list.map { it.title })
            assertEquals(listOf(BookFileFormat.TEXT, BookFileFormat.EPUB), list.map { it.format })
            assertEquals(listOf(250L, 100L), list.map { it.sizeBytes })
        }

    @Test
    fun `totals count each book once and add every file`() =
        runTest {
            download("1", "Both", "epub", 100, at = 1L)
            val bookId = storage.observe().first().single().bookId
            val pdf = File(booksDir, "1.pdf").also { it.writeBytes(ByteArray(40)) }
            database.downloadedBookDao().insert(
                DownloadedBook(bookId = bookId, localFilePath = pdf.path, format = "pdf", downloadedAt = 3L),
            )

            val state = StorageState(storage.observe().first())

            assertEquals(1, state.bookCount)
            assertEquals(140L, state.totalBytes)
        }

    @Test
    fun `a record whose file is gone shows with size zero`() =
        runTest {
            val file = download("1", "Missing", "epub", 100, at = 1L)
            file.delete()

            assertEquals(0L, storage.observe().first().single().sizeBytes)
        }

    @Test
    fun `delete removes the file and the record`() =
        runTest {
            val file = download("1", "Gone", "epub", 100, at = 1L)
            val keep = download("2", "Kept", "epub", 100, at = 2L)

            storage.delete(storage.observe().first().first { it.title == "Gone" })

            assertFalse(file.exists())
            assertEquals(listOf("Kept"), storage.observe().first().map { it.title })
            assertEquals(true, keep.exists())
        }
}
