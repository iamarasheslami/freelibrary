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

/** One line of a book's contents: its [title] (may be empty), the [chapter] it opens (an index into the chapters) and how deeply it is [level] nested. */
data class ContentsEntry(
    val title: String,
    val chapter: Int,
    val level: Int,
)

/**
 * An EPUB file opened for reading. Nothing is unpacked: every file inside is read from the zip
 * when it is asked for, so opening is quick and nothing is left on the phone. [chapters] are the
 * paths of the book's pages in reading order; [contents] is the book's own table of contents,
 * limited to lines that open one of those chapters, and empty if the book has none. The book must
 * be closed when the reader is done.
 */
class EpubBook private constructor(
    private val zip: ZipFile,
    val title: String?,
    val chapters: List<String>,
    val contents: List<ContentsEntry>,
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
        private const val NCX = "application/x-dtbncx+xml"
        private const val MAX_LEVEL = 3

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
                val contents = readContents(zip, document, chapters)
                EpubOpenResult.Opened(EpubBook(zip, document.title, chapters, contents, mediaTypes))
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

        /**
         * The book's contents: from its navigation document if that gives any line that opens a
         * chapter, otherwise from its NCX file. A contents file that is missing or damaged only
         * means there is no contents; it never stops the book from opening.
         */
        private fun readContents(
            zip: ZipFile,
            document: PackageDocument,
            chapters: List<String>,
        ): List<ContentsEntry> {
            val navigation = document.items.values.firstOrNull { it.hasProperty("nav") }
            val ncx = document.tocId?.let { document.items[it] } ?: document.items.values.firstOrNull { it.mediaType == NCX }
            for ((item, isNavigation) in listOfNotNull(navigation?.let { it to true }, ncx?.let { it to false })) {
                val raw =
                    try {
                        val entry = zip.getEntry(item.path)
                        if (entry == null) {
                            emptyList()
                        } else {
                            zip.getInputStream(entry).use {
                                if (isNavigation) EpubContentsParser.parseNav(it, item.path) else EpubContentsParser.parseNcx(it, item.path)
                            }
                        }
                    } catch (e: XmlPullParserException) {
                        emptyList()
                    } catch (e: IOException) {
                        emptyList()
                    }
                val entries =
                    raw.mapNotNull { line ->
                        val chapter = chapters.indexOf(line.path)
                        if (chapter < 0) null else ContentsEntry(line.title, chapter, line.level.coerceAtMost(MAX_LEVEL))
                    }
                if (entries.isNotEmpty()) return entries
            }
            return emptyList()
        }

        private fun isPage(item: ManifestItem): Boolean =
            when (item.mediaType?.lowercase()) {
                "application/xhtml+xml", "text/html" -> true
                null -> EpubPackageParser.mimeTypeForName(item.path).let { it == "application/xhtml+xml" || it == "text/html" }
                else -> false
            }
    }
}
