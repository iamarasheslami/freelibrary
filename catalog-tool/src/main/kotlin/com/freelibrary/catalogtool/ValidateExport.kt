package com.freelibrary.catalogtool

import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * Checks an export folder before it is published (see [validateExport]). Arguments: the export folder, and
 * optionally `--previous <folder>` holding the currently published manifest.json. Exits with status 1 when
 * anything is wrong, so a workflow stops before it pushes. Run via `./gradlew :catalog-tool:validateExport`.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: validateExport <export folder> [--previous <folder with the published manifest.json>]")
        exitProcess(2)
    }
    val exportDir = Path.of(args[0])
    val previousDir = args.indexOf("--previous").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }?.let { Path.of(it) }
    val previous =
        previousDir?.let {
            if (!Files.isRegularFile(it.resolve("manifest.json"))) {
                println("No published manifest at $it; checking the export on its own.")
                null
            } else {
                readManifest(it)
            }
        }

    val report = validateExport(exportDir, previous)
    println("Checked ${report.bookCount} book(s): ${report.errorCount} error(s), ${report.warningCount} warning(s).")
    report.warnings.forEach { println("  warning: $it") }
    report.errors.forEach { println("  ERROR: $it") }
    if (!report.ok) exitProcess(1)
}
