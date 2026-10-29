package com.freelibrary.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.freelibrary.app.data.local.FreeLibraryDatabase
import com.freelibrary.app.data.local.dao.BookFormatDao
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookBookshelf
import com.freelibrary.app.data.local.entity.BookFormat
import com.freelibrary.app.data.local.entity.Bookshelf
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
import org.junit.Assert.assertNull
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

    private fun buildRepository(
        dispatcher: Dispatcher,
        sources: CatalogSources = CatalogSources(),
        bookFormatDao: BookFormatDao = database.bookFormatDao(),
    ): CatalogSyncRepository {
        server.dispatcher = dispatcher
        val api = createCatalogSyncApi(baseUrl = server.url("/").toString())
        return CatalogSyncRepository(
            database = database,
            api = api,
            sourceDao = database.sourceDao(),
            bookDao = database.bookDao(),
            authorDao = database.authorDao(),
            bookAuthorDao = database.bookAuthorDao(),
            subjectDao = database.subjectDao(),
            bookSubjectDao = database.bookSubjectDao(),
            bookshelfDao = database.bookshelfDao(),
            bookBookshelfDao = database.bookBookshelfDao(),
            bookFormatDao = bookFormatDao,
            bookFtsDao = database.bookFtsDao(),
            bookSummaryDao = database.bookSummaryDao(),
            sources = sources,
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

    @Test
    fun `a manifest entry with an unsafe id is counted as a failure and never requested`() =
        runTest {
            val manifest =
                """
                { "generatedAt": "2026-09-19", "baselineVersion": "2026-09-19",
                  "books": [ { "externalId": "../secret", "lastModified": "2026-09-19" },
                             { "externalId": "158", "lastModified": "2026-09-19" } ] }
                """.trimIndent()
            val requested = mutableListOf<String>()
            val dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        requested += request.path.orEmpty()
                        return when (request.path) {
                            "/manifest.json" -> MockResponse().setBody(manifest).setHeader("Content-Type", "application/json")
                            "/books/158.json" ->
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
            assertEquals(listOf("/manifest.json", "/books/158.json"), requested)
        }

    @Test
    fun `a book file that holds a different book than the manifest names is rejected`() =
        runTest {
            val dispatcher =
                serving(
                    manifest = { manifestFor("1342", "2026-09-19") },
                    book = { bookJson("158", "Emma", "2026-09-19") },
                )
            repository = buildRepository(dispatcher)

            val result = repository.sync()

            assertEquals(0, result.added)
            assertEquals(1, result.failed)
            assertEquals(0, database.bookDao().count())
        }

    private fun manifestFor(
        externalId: String,
        lastModified: String,
    ) = """
        { "generatedAt": "$lastModified", "baselineVersion": "2026-09-19",
          "books": [ { "externalId": "$externalId", "lastModified": "$lastModified" } ] }
        """.trimIndent()

    private fun richBookJson(
        externalId: String,
        title: String,
        creator: String,
        lastModified: String,
        summary: String? = null,
        coverUrl: String? = null,
    ): String {
        val summaryField = summary?.let { ", \"summary\": \"$it\"" }.orEmpty()
        val coverField = coverUrl?.let { ", \"coverUrl\": \"$it\"" }.orEmpty()
        return """
            {
              "externalId": "$externalId",
              "title": "$title",
              "issuedDate": null,
              "language": "en",
              "locc": null,
              "creators": [{ "name": "$creator", "birthYear": null, "deathYear": null }],
              "subjects": [],
              "bookshelves": [],
              "formats": [],
              "lastModified": "$lastModified"$summaryField$coverField
            }
            """.trimIndent()
    }

    private fun serving(
        manifest: () -> String,
        book: () -> String,
    ) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            when (request.path) {
                "/manifest.json" ->
                    MockResponse().setBody(manifest()).setHeader("Content-Type", "application/json")
                "/books/1342.json" ->
                    MockResponse().setBody(book()).setHeader("Content-Type", "application/json")
                else -> MockResponse().setResponseCode(404)
            }
    }

    private suspend fun savedBook(): Book {
        val sourceId = database.sourceDao().findByName("Project Gutenberg")!!.id
        return database.bookDao().getBySourceAndExternalId(sourceId, "1342")!!
    }

    @Test
    fun `sync saves the cover URL and summary and indexes folded text`() =
        runTest {
            val book =
                richBookJson(
                    "1342",
                    "Les Misérables",
                    "Hugo, Victor",
                    "2026-10-02",
                    summary = "Un roman célèbre.",
                    coverUrl = "https://example.org/1342.cover.medium.jpg",
                )
            repository = buildRepository(serving({ manifestFor("1342", "2026-10-02") }, { book }))

            repository.sync()

            val saved = savedBook()
            assertEquals("https://example.org/1342.cover.medium.jpg", saved.coverUrl)
            assertEquals("Un roman célèbre.", database.bookSummaryDao().getSummaryForBook(saved.id))
            assertEquals(listOf("les miserables"), database.bookFtsDao().allIndexedTitles())
            assertEquals(listOf("hugo victor"), database.bookFtsDao().allIndexedAuthorNames())
        }

    @Test
    fun `a book with no cover or summary is saved with neither`() =
        runTest {
            val book = richBookJson("1342", "Emma", "Austen, Jane", "2026-10-02")
            repository = buildRepository(serving({ manifestFor("1342", "2026-10-02") }, { book }))

            repository.sync()

            val saved = savedBook()
            assertNull(saved.coverUrl)
            assertNull(database.bookSummaryDao().getSummaryForBook(saved.id))
        }

    @Test
    fun `a later sync updates the cover and removes a summary the catalog no longer has`() =
        runTest {
            var manifest = manifestFor("1342", "2026-10-02")
            var book =
                richBookJson(
                    "1342",
                    "Emma",
                    "Austen, Jane",
                    "2026-10-02",
                    summary = "Old summary.",
                    coverUrl = "https://example.org/old.jpg",
                )
            repository = buildRepository(serving({ manifest }, { book }))
            repository.sync()

            manifest = manifestFor("1342", "2026-10-03")
            book = richBookJson("1342", "Emma", "Austen, Jane", "2026-10-03", coverUrl = "https://example.org/new.jpg")
            repository.sync()

            val saved = savedBook()
            assertEquals("https://example.org/new.jpg", saved.coverUrl)
            assertNull(database.bookSummaryDao().getSummaryForBook(saved.id))
        }

    private val oapen =
        CatalogSource(
            name = "OAPEN Library",
            attribution = "Open access books via OAPEN (https://www.oapen.org)",
            folder = "sources/oapen/",
        )

    private fun json(body: String) = MockResponse().setBody(body).setHeader("Content-Type", "application/json")

    /** Both sources hold a book numbered 1342; [oapenManifest] is what the second source's manifest answers. */
    private fun twoSources(oapenManifest: () -> MockResponse) =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                when (request.path) {
                    "/manifest.json" -> json(manifestFor("1342", "2026-10-02"))
                    "/books/1342.json" -> json(richBookJson("1342", "Gutenberg book", "Austen, Jane", "2026-10-02"))
                    "/sources/oapen/manifest.json" -> oapenManifest()
                    "/sources/oapen/books/1342.json" -> json(richBookJson("1342", "OAPEN book", "Smith, Ann", "2026-10-02"))
                    else -> MockResponse().setResponseCode(404)
                }
        }

    @Test
    fun `a second source is synced from its own folder and kept apart from the first`() =
        runTest {
            val sources = CatalogSources(listOf(CatalogSource.GUTENBERG, oapen))
            repository = buildRepository(twoSources { json(manifestFor("1342", "2026-10-02")) }, sources)

            val result = repository.sync()

            assertEquals(2, result.added)
            val gutenbergId = database.sourceDao().findByName("Project Gutenberg")!!.id
            val oapenId = database.sourceDao().findByName("OAPEN Library")!!.id
            assertEquals("Gutenberg book", database.bookDao().getBySourceAndExternalId(gutenbergId, "1342")?.title)
            assertEquals("OAPEN book", database.bookDao().getBySourceAndExternalId(oapenId, "1342")?.title)
            assertEquals(2, database.bookDao().count())
        }

    @Test
    fun `a second source that cannot be fetched is one failure and the first source still syncs`() =
        runTest {
            val sources = CatalogSources(listOf(CatalogSource.GUTENBERG, oapen))
            repository = buildRepository(twoSources { MockResponse().setResponseCode(500) }, sources)

            val result = repository.sync()

            assertEquals(1, result.added)
            assertEquals(1, result.failed)
            assertEquals(listOf("OAPEN Library: Failed to fetch sync manifest"), result.failureSamples)
        }

    @Test
    fun `a book that fails half way through its update keeps its old data and its old date`() =
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
            val shelfId = database.bookshelfDao().insert(Bookshelf(name = "Old Shelf"))
            database.bookBookshelfDao().insert(BookBookshelf(bookId = existingId, bookshelfId = shelfId))
            database.bookFormatDao().insert(
                BookFormat(bookId = existingId, formatType = "epub", downloadUrl = "https://example.org/old.epub"),
            )
            val failingFormats =
                object : BookFormatDao by database.bookFormatDao() {
                    override suspend fun insert(bookFormat: BookFormat): Long = error("the disk is full")
                }
            val manifest =
                """{ "generatedAt": "2026-09-19", "baselineVersion": "2026-09-19",
                     "books": [ { "externalId": "1342", "lastModified": "2026-09-19" } ] }"""
            val dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse =
                        when (request.path) {
                            "/manifest.json" -> MockResponse().setBody(manifest).setHeader("Content-Type", "application/json")
                            "/books/1342.json" ->
                                MockResponse().setBody(bookJson("1342", "Pride and Prejudice (corrected)", "2026-09-19"))
                                    .setHeader("Content-Type", "application/json")
                            else -> MockResponse().setResponseCode(404)
                        }
                }
            repository = buildRepository(dispatcher, bookFormatDao = failingFormats)

            val result = repository.sync()

            assertEquals(1, result.failed)
            val book = database.bookDao().getById(existingId)
            assertEquals("Pride and Prejudice (old title)", book?.title)
            assertEquals("2026-09-01", book?.lastModified)
            assertEquals(listOf("Old Shelf"), database.bookBookshelfDao().getBookshelvesForBook(existingId).map { it.name })
            assertEquals(listOf("epub"), database.bookFormatDao().getFormatsForBook(existingId).map { it.formatType })
        }
}
