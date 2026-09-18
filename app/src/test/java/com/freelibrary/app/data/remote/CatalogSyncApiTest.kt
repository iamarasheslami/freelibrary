package com.freelibrary.app.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CatalogSyncApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: CatalogSyncApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = createCatalogSyncApi(baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getManifest parses a real manifest response`() =
        runTest {
            val responseBody =
                """
                {
                  "generatedAt": "2026-09-16",
                  "baselineVersion": "2026-09-16",
                  "books": [
                    { "externalId": "1342", "lastModified": "2026-09-16" }
                  ]
                }
                """.trimIndent()
            server.enqueue(MockResponse().setBody(responseBody).setHeader("Content-Type", "application/json"))

            val manifest = api.getManifest()

            assertEquals("2026-09-16", manifest.baselineVersion)
            assertEquals(1, manifest.books.size)
            assertEquals("1342", manifest.books.first().externalId)
        }

    @Test
    fun `getBook parses a real book export response, ignoring unknown fields`() =
        runTest {
            val responseBody =
                """
                {
                  "externalId": "1342",
                  "title": "Pride and Prejudice",
                  "issuedDate": "1998-06-01",
                  "language": "en",
                  "locc": "PR",
                  "creators": [{ "name": "Jane Austen", "birthYear": 1775, "deathYear": 1817 }],
                  "subjects": ["Fiction"],
                  "bookshelves": ["Best Books Ever Listings"],
                  "formats": [{ "url": "https://example.org/1342.txt", "formatType": "txt" }],
                  "lastModified": "2026-09-16",
                  "someFutureFieldThisAppVersionDoesNotKnowAbout": "should be safely ignored"
                }
                """.trimIndent()
            server.enqueue(MockResponse().setBody(responseBody).setHeader("Content-Type", "application/json"))

            val book = api.getBook("1342")

            assertEquals("Pride and Prejudice", book.title)
            assertEquals("Jane Austen", book.creators.first().name)
        }

    @Test
    fun `getBook requests the correct path for the given external id`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """{"externalId":"158","title":"Emma","issuedDate":null,"language":null,"locc":null,""" +
                        """"creators":[],"subjects":[],"bookshelves":[],"formats":[],"lastModified":"2026-09-16"}""",
                ).setHeader("Content-Type", "application/json"),
            )

            api.getBook("158")

            val recordedRequest = server.takeRequest()
            assertEquals("/books/158.json", recordedRequest.path)
        }
}
