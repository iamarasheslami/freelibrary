package com.freelibrary.catalogtool

import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager

/**
 * Standalone entry point for analyzing an already-generated catalog.sqlite,
 * without re-running the full download/parse/write pipeline. Run via
 * `./gradlew :catalog-tool:inspect`.
 */
fun main() {
    val outputPath = Path.of("data/catalog.sqlite")

    if (!Files.exists(outputPath)) {
        println("No database found at $outputPath. Run the main pipeline first.")
        return
    }

    DriverManager.getConnection("jdbc:sqlite:$outputPath").use { connection ->
        printDatabaseSizeReport(connection)
    }
}
