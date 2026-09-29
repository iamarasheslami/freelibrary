package com.freelibrary.catalogtool

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Thrown when a simple HTTP GET fails or returns a non-200 status. */
class HttpFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Redirects must be followed explicitly: java.net.http.HttpClient's default
 * policy is Redirect.NEVER, so without this every redirecting URL (such as
 * Gutenberg's per-book RDF endpoints) looks like a failure.
 */
private val sharedHttpClient: HttpClient =
    HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

/**
 * Fetches [url] and returns its raw bytes. Unlike [downloadCatalogArchive],
 * this never caches to disk - suited to small, one-off fetches like an RSS
 * feed or a single book's RDF file, not the large full archive.
 */
fun fetchBytes(url: String): ByteArray {
    val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()

    val response =
        try {
            sharedHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        } catch (e: Exception) {
            throw HttpFetchException("Failed to fetch $url: ${e.javaClass.simpleName} - ${e.message}", e)
        }

    if (response.statusCode() != 200) {
        throw HttpFetchException("Fetching $url returned HTTP ${response.statusCode()} instead of 200")
    }

    return response.body()
}
