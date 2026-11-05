package com.freelibrary.catalogtool

import java.nio.file.Path
import java.time.LocalDate
import kotlin.system.exitProcess

/**
 * Takes books out of the catalog (see [withdrawBooks]). Arguments: the export folder holding manifest.json and
 * books/ (a clone of the data repository), then the ids of the books to withdraw. Exits with status 1 when an id
 * is unknown or unsafe, so a mistake is noticed. Run via `./gradlew :catalog-tool:withdrawBooks`.
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: withdrawBooks <export folder> <book id> [<book id> ...]")
        exitProcess(2)
    }
    val result = withdrawBooks(Path.of(args[0]), args.drop(1), LocalDate.now().toString())
    println("Withdrawn: ${result.withdrawn.size}")
    result.withdrawn.forEach { println("  $it") }
    result.alreadyWithdrawn.forEach { println("  already withdrawn: $it") }
    result.unknown.forEach { println("  ERROR: $it is not in the catalog") }
    result.unsafe.forEach { println("  ERROR: \"$it\" is not a book id") }
    if (result.unknown.isNotEmpty() || result.unsafe.isNotEmpty()) exitProcess(1)
}
