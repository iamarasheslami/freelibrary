package com.freelibrary.catalogtool

import java.nio.file.Files
import java.nio.file.Path

/** What [withdrawBooks] did with the ids it was given. */
data class WithdrawalResult(
    /** Books that were listed and are now withdrawn. */
    val withdrawn: List<String>,
    /** Ids that were withdrawn before; nothing changed for them. */
    val alreadyWithdrawn: List<String>,
    /** Ids that are neither listed nor withdrawn, probably a typing mistake; nothing changed for them. */
    val unknown: List<String>,
    /** Ids that are not a safe book id; nothing changed for them. */
    val unsafe: List<String>,
)

/**
 * Withdraws the books [ids] from the export in [exportDir] (manifest.json and books/): each one leaves the manifest's
 * book list, its file is deleted, and its id joins the manifest's withdrawn list, which apps use to remove the book
 * from their own catalog. Nothing is written when there is nothing to withdraw. Ids that are unknown, unsafe or
 * already withdrawn are only reported.
 */
fun withdrawBooks(
    exportDir: Path,
    ids: List<String>,
    today: String,
): WithdrawalResult {
    val manifest = readManifest(exportDir) ?: error("There is no manifest.json in $exportDir")
    val listed = manifest.books.map { it.externalId }.toSet()
    val already = manifest.withdrawn.toSet()
    val unsafe = ids.filterNot { BOOK_ID.matches(it) }.distinct()
    val safe = ids.filter { BOOK_ID.matches(it) }.distinct()
    val toWithdraw = safe.filter { it in listed }
    val alreadyWithdrawn = safe.filter { it !in listed && it in already }
    val unknown = safe.filter { it !in listed && it !in already }

    if (toWithdraw.isNotEmpty()) {
        val leaving = toWithdraw.toSet()
        writeManifest(
            exportDir,
            manifest.copy(
                generatedAt = today,
                books = manifest.books.filterNot { it.externalId in leaving },
                withdrawn = (manifest.withdrawn + toWithdraw).distinct().sorted(),
            ),
        )
        toWithdraw.forEach { Files.deleteIfExists(exportDir.resolve("books").resolve("$it.json")) }
    }
    return WithdrawalResult(toWithdraw, alreadyWithdrawn, unknown, unsafe)
}
