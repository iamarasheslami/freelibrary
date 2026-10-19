package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.TimeProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.DownloadedBook
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.domain.model.ReaderPosition
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
class ReaderRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: FreeLibraryDatabase
    private lateinit var repository: ReaderRepository
    private var now = 5_000L
    private var bookId: Long = 0

    @Before
    fun setUp() =
        runBlocking {
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
                        externalId = "2701",
                        title = "Moby Dick",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-10-16",
                    ),
                )
            repository = ReaderRepository(database.downloadedBookDao(), database.readingProgressDao(), TimeProvider { now })
        }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun download(
        format: String,
        file: File,
    ) {
        database.downloadedBookDao().insert(DownloadedBook(bookId = bookId, localFilePath = file.path, format = format, downloadedAt = 1L))
    }

    @Test
    fun `the downloaded EPUB is found when its file is there`() =
        runBlocking<Unit> {
            val file = temporaryFolder.newFile("$bookId.epub")
            download("epub", file)

            assertEquals(file, repository.epubFile(bookId))
        }

    @Test
    fun `an EPUB whose file has gone is not found`() =
        runBlocking<Unit> {
            download("epub", File(temporaryFolder.root, "gone.epub"))

            assertNull(repository.epubFile(bookId))
        }

    @Test
    fun `a book downloaded in another format has no EPUB`() =
        runBlocking<Unit> {
            download("pdf", temporaryFolder.newFile("$bookId.pdf"))

            assertNull(repository.epubFile(bookId))
        }

    @Test
    fun `a book that was never downloaded has no EPUB`() =
        runBlocking<Unit> {
            assertNull(repository.epubFile(bookId))
        }

    @Test
    fun `with nothing saved there is no position`() =
        runBlocking<Unit> {
            assertNull(repository.savedEpubPosition(bookId))
        }

    @Test
    fun `a saved position is read back`() =
        runBlocking<Unit> {
            assertTrue(repository.savePosition(bookId, ReaderPosition.Epub(12, 250)))

            assertEquals(ReaderPosition.Epub(12, 250), repository.savedEpubPosition(bookId))
        }

    @Test
    fun `saving again replaces the position and records the time`() =
        runBlocking<Unit> {
            repository.savePosition(bookId, ReaderPosition.Epub(1, 100))
            now = 9_000L
            repository.savePosition(bookId, ReaderPosition.Epub(2, 900))

            assertEquals(ReaderPosition.Epub(2, 900), repository.savedEpubPosition(bookId))
            val stored = database.readingProgressDao().getForBook(bookId)
            assertNotNull(stored)
            assertEquals("epub:2:900", stored!!.lastPosition)
            assertEquals(9_000L, stored.lastOpenedAt)
        }

    @Test
    fun `a position of another kind or one that cannot be read is not an EPUB position`() =
        runBlocking<Unit> {
            database.readingProgressDao().upsert(ReadingProgress(bookId, "pdf:7", 1L))
            assertNull(repository.savedEpubPosition(bookId))

            database.readingProgressDao().upsert(ReadingProgress(bookId, "garbage", 1L))
            assertNull(repository.savedEpubPosition(bookId))
        }

    @Test
    fun `a position cannot be saved for a book the catalog no longer has`() =
        runBlocking<Unit> {
            assertFalse(repository.savePosition(bookId + 1000, ReaderPosition.Epub(0, 0)))
        }
}
