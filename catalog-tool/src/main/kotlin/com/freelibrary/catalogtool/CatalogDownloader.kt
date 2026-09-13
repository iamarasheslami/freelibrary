package com.freelibrary.catalogtool

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

private const val CATALOG_ARCHIVE_URL = "https://www.gutenberg.org/cache/epub/feeds/rdf-files.tar.bz2"

/**
 * Thrown when the catalog archive cannot be downloaded successfully, carrying
 * enough context to diagnose the failure without guessing.
 */
class CatalogDownloadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Downloads Project Gutenberg's full RDF catalog archive to [destination].
 * If a file already exists at [destination], it is reused as-is rather than
 * re-downloaded - a development convenience, since this archive is large.
 *
 * Streams directly to disk rather than buffering in memory, since the
 * archive is expected to be several hundred megabytes.
 */
fun downloadCatalogArchive(destination: Path): Path {
    if (Files.exists(destination)) {
        println("Using existing cached archive at $destination (delete it to force a fresh download).")
        return destination
    }

    Files.createDirectories(destination.parent)

    val client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build()

    val request =
        HttpRequest.newBuilder()
            .uri(URI.create(CATALOG_ARCHIVE_URL))
            .GET()
            .build()

    println("Downloading catalog archive from $CATALOG_ARCHIVE_URL ...")

    val response =
        try {
            client.send(request, HttpResponse.BodyHandlers.ofFile(destination))
        } catch (e: Exception) {
            throw CatalogDownloadException("Failed to download catalog archive from $CATALOG_ARCHIVE_URL", e)
        }

    if (response.statusCode() != 200) {
        Files.deleteIfExists(destination)
        throw CatalogDownloadException(
            "Catalog archive download returned HTTP ${response.statusCode()} instead of 200",
        )
    }

    println("Downloaded catalog archive to $destination")
    return destination
}
