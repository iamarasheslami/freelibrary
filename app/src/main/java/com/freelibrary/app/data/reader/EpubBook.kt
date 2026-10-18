package com.freelibrary.app.data.reader

import org.xmlpull.v1.XmlPullParserException
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

/** Why a file could not be opened as an EPUB. */
enum class EpubProblem {
    /** Not a zip file, or a zip file without the container an EPUB must have. */
    NOT_AN_EPUB,

    /** An EPUB whose description is missing, unreadable or points at nothing. */
    DAMAGED,

    /** An EPUB with no chapter to read. */
    EMPTY,
}

sealed interface EpubOpenResult {
    data class Opened(
        val book: EpubBook,
    ) : EpubOpenResult

    data class Failed(
        val problem: EpubProblem,
    ) : EpubOpenResult
}

/**
 * An EPUB file opened for reading. Nothing is unpacked: every file inside is read from the zip
 * when it is asked for, so opening is quick and nothing is left on the phone. [chapters] are the
 * paths of the book's pages in reading order. The book must be closed when the reader is done.
 */
class EpubBook private constructor(
    private val zip: ZipFile,
    val title: String?,
    val chapters: List<String>,
    private val mediaTypes: Map<String, String>,
) : Closeable {
    /** The content of the file at [path] inside the book, or null if there is none. The caller closes it. */
    fun read(path: String): InputStream? {
        val entry = zip.getEntry(path) ?: return null
        return zip.getInputStream(entry)
    }

    /** The media type of the file at [path]: as the book lists it, else guessed from the name. */
    fun mimeType(path: String): String = mediaTypes[path] ?: EpubPackageParser.mimeTypeForName(path)

    override fun close() = zip.close()

    companion object {
        /** Opens [file]. Reads files from disk, so call it off the main thread. */
        fun open(file: File): EpubOpenResult {
            val zip =
                try {
                    ZipFile(file)
                } catch (e: IOException) {
                    return EpubOpenResult.Failed(EpubProblem.NOT_AN_EPUB)
                }
            var result: EpubOpenResult? = null
            try {
                result = load(zip)
            } finally {
                if (result !is EpubOpenResult.Opened) zip.close()
            }
            return result
        }

        private fun load(zip: ZipFile): EpubOpenResult {
            val container = zip.getEntry("META-INF/container.xml") ?: return EpubOpenResult.Failed(EpubProblem.NOT_AN_EPUB)
            return try {
                val packagePath =
                    zip.getInputStream(container).use { EpubPackageParser.rootFilePath(it) }
                        ?: return EpubOpenResult.Failed(EpubProblem.DAMAGED)
                val packageEntry = zip.getEntry(packagePath) ?: return EpubOpenResult.Failed(EpubProblem.DAMAGED)
                val document = zip.getInputStream(packageEntry).use { EpubPackageParser.parsePackage(it, packagePath) }
                val chapters = readingOrder(zip, document)
                if (chapters.isEmpty()) return EpubOpenResult.Failed(EpubProblem.EMPTY)
                val mediaTypes = document.items.values.mapNotNull { item -> item.mediaType?.let { item.path to it } }.toMap()
                EpubOpenResult.Opened(EpubBook(zip, document.title, chapters, mediaTypes))
            } catch (e: XmlPullParserException) {
                EpubOpenResult.Failed(EpubProblem.DAMAGED)
            } catch (e: IOException) {
                EpubOpenResult.Failed(EpubProblem.DAMAGED)
            }
        }

        /** The pages in reading order. Material marked as outside the main path is left out unless nothing else is left. */
        private fun readingOrder(
            zip: ZipFile,
            document: PackageDocument,
        ): List<String> {
            val references = document.spine.filter { it.linear }.ifEmpty { document.spine }
            return references
                .mapNotNull { document.items[it.idref] }
                .filter { isPage(it) && zip.getEntry(it.path) != null }
                .map { it.path }
                .distinct()
        }

        private fun isPage(item: ManifestItem): Boolean =
            when (item.mediaType?.lowercase()) {
                "application/xhtml+xml", "text/html" -> true
                null -> EpubPackageParser.mimeTypeForName(item.path).let { it == "application/xhtml+xml" || it == "text/html" }
                else -> false
            }
    }
}
