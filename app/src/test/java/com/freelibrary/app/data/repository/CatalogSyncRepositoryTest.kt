package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.ReadingProgress
import com.freelibrary.app.data.local.entity.Source
import com.freelibrary.app.data.remote.createCatalogSyncApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogSyncRepositoryTest {
    private lateinit var database: FreeLibraryDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: CatalogSyncRepository

    private val manifestJson =
        """
        {
          "generatedAt": "2026-09-19",
          "baselineVersion": "2026-09-19",
          "books": [
            { "externalId": "1342", "lastModified": "2026-09-19" },
            { "externalId": "158", "lastModified": "2026-09-19" }
          ]
        }
        """.trimIndent()

    private fun bookJson(
        externalId: String,
        title: String,
        lastModified: String,
    ) = """
        {
          "externalId": "$externalId",
          "title": "$title",
          "issuedDate": "1998-06-01",
          "language": "en",
          "locc": "PR",
          "creators": [{ "name": "Jane Austen", "birthYear": 1775, "deathYear": 1817 }],
          "subjects": ["Fiction"],
          "bookshelves": ["Best Books Ever Listings"],
          "formats": [{ "url": "https://example.org/$externalId.txt", "formatType": "txt" }],
          "lastModified": "$lastModified"
        }
        """.trimIndent()

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                FreeLibraryDatabase::class.java,
            ).allowMainThreadQueries().build()

        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    private fun buildRepository(dispatcher: Dispatcher): CatalogSyncRepository {
        server.dispatcher = dispatcher
        val api = createCatalogSyncApi(baseUrl = server.url("/").toString())
        return CatalogSyncRepository(
            api = api,
            sourceDao = database.sourceDao(),
            bookDao = database.bookDao(),
            authorDao = database.authorDao(),
            bookAuthorDao = database.bookAuthorDao(),
            subjectDao = database.subjectDao(),
            bookSubjectDao = database.bookSubjectDao(),
            bookshelfDao = database.bookshelfDao(),
            bookBookshelfDao = database.bookBookshelfDao(),
            bookFormatDao = database.bookFormatDao(),
            bookFtsDao = database.bookFtsDao(),
        )
    }

    @Test
    fun `sync inserts new books with all associations`() =
        runTest {
            val dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        return when {
                            request.path == "/manifest.json" ->
                                MockResponse().setBody(manifestJson).setHeader("Content-Type", "application/json")
                            request.path == "/books/1342.json" ->
                                MockResponse().setBody(bookJson("1342", "Pride and Prejudice", "2026-09-19"))
                                    .setHeader("Content-Type", "application/json")
                            request.path == "/books/158.json" ->
                                MockResponse().setBody(bookJson("158", "Emma", "2026-09-19"))
                                    .setHeader("Content-Type", "application/json")
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }
            repository = buildRepository(dispatcher)

            val result = repository.sync()

            assertEquals(2, result.added)
            assertEquals(0, result.updated)
            assertEquals(0, result.failed)
            assertEquals(2, database.bookDao().count())
        }

    @Test
    fun `sync updates an existing book in place, preserving its internal id and reading progress`() =
        runTest {
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            val existingId =
                database.bookDao().insert(
                    Book(
                        sourceId = sourceId,
                        externalId = "1342",
                        title = "Pride and Prejudice (old title)",
                        issuedDate = null,
                        primaryLanguage = "en",
                        locc = null,
                        lastModified = "2026-09-01",
                    ),
                )
            database.readingProgressDao().upsert(
                ReadingProgress(bookId = existingId, lastPosition = "para-100", lastOpenedAt = 1000L),
            )

            val dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        return when {
                            request.path == "/manifest.json" ->
                                MockResponse().setBody(manifestJson).setHeader("Content-Type", "application/json")
                            request.path == "/books/1342.json" ->
                                MockResponse().setBody(bookJson("1342", "Pride and Prejudice (corrected)", "2026-09-19"))
                                    .setHeader("Content-Type", "application/json")
                            request.path == "/books/158.json" ->
                                MockResponse().setBody(bookJson("158", "Emma", "2026-09-19"))
                                    .setHeader("Content-Type", "application/json")
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }
            repository = buildRepository(dispatcher)

            val result = repository.sync()

            assertEquals(1, result.added)
            assertEquals(1, result.updated)

            val updatedBook = database.bookDao().getById(existingId)
            assertEquals(existingId, updatedBook?.id)
            assertEquals("Pride and Prejudice (corrected)", updatedBook?.title)

            val survivingProgress = database.readingProgressDao().getForBook(existingId)
            assertNotNull("Reading progress must survive a sync-driven book update", survivingProgress)
            assertEquals("para-100", survivingProgress?.lastPosition)
        }

    @Test
    fun `sync skips a book whose lastModified already matches the local copy`() =
        runTest {
            val sourceId = database.sourceDao().insert(Source(name = "Project Gutenberg", attribution = "PG"))
            database.bookDao().insert(
                Book(
                    sourceId = sourceId,
                    externalId = "1342",
                    title = "Pride and Prejudice",
                    issuedDate = null,
                    primaryLanguage = "en",
                    locc = null,
                    lastModified = "2026-09-19",
                ),
            )

            val dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        return when {
                            request.path == "/manifest.json" ->
                                MockResponse().setBody(manifestJson).setHeader("Content-Type", "application/json")
                            request.path == "/books/158.json" ->
                                MockResponse().setBody(bookJson("158", "Emma", "2026-09-19"))
                                    .setHeader("Content-Type", "application/json")
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }
            repository = buildRepository(dispatcher)

            val result = repository.sync()

            assertEquals(1, result.added)
            assertEquals(0, result.updated)
            assertEquals("/manifest.json", server.takeRequest().path)
            assertEquals("/books/158.json", server.takeRequest().path)
        }

    @Test
    fun `a single failing book does not abort the whole sync`() =
        runTest {
            val dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        return when {
                            request.path == "/manifest.json" ->
                                MockResponse().setBody(manifestJson).setHeader("Content-Type", "application/json")
                            request.path == "/books/1342.json" -> MockResponse().setResponseCode(500)
                            request.path == "/books/158.json" ->
                                MockResponse().setBody(bookJson("158", "Emma", "2026-09-19"))
                                    .setHeader("Content-Type", "application/json")
                            else -> MockResponse().setResponseCode(404)
                        }
                    }
                }
            repository = buildRepository(dispatcher)

            val result = repository.sync()

            assertEquals(1, result.added)
            assertEquals(1, result.failed)
            assertEquals(1, result.failureSamples.size)
        }
}
