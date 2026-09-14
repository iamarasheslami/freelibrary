package com.freelibrary.catalogtool

import java.nio.file.Path

/**
 * Entry point for the catalog-generation tool. This is a standalone JVM
 * command-line program - it is never shipped inside the Android app. Its job
 * is to download Project Gutenberg's RDF catalog and produce a SQLite
 * database matching the app's Room schema.
 */
fun main() {
    val archivePath = Path.of("data/rdf-files.tar.bz2")

    try {
        downloadCatalogArchive(archivePath)
    } catch (e: CatalogDownloadException) {
        println("Catalog download failed: ${e.message}")
        return
    }

    var rdfFileCount = 0
    readRdfEntriesFromArchive(archivePath) { _, _ ->
        rdfFileCount++
    }

    println("Found $rdfFileCount RDF entries in the archive.")
}
