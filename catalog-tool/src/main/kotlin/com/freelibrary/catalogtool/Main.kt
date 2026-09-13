package com.freelibrary.catalogtool

/**
 * Entry point for the catalog-generation tool. This is a standalone JVM
 * command-line program - it is never shipped inside the Android app. Its job
 * is to download Project Gutenberg's RDF catalog and produce a SQLite
 * database matching the app's Room schema.
 *
 * This initial version only proves the build/run pipeline works; real
 * catalog-generation logic is added incrementally from here.
 */
fun main() {
    println("FreeLibrary catalog-tool: pipeline check successful.")
}
