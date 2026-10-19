package com.freelibrary.app.data.reader

import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/** One file the book lists, with its path inside the EPUB and the properties the book gives it. */
internal data class ManifestItem(
    val id: String,
    val path: String,
    val mediaType: String?,
    val properties: String? = null,
) {
    fun hasProperty(name: String): Boolean = properties?.trim()?.split(Regex("\\s+"))?.contains(name) == true
}

/** One entry of the reading order; [linear] is false for material outside the main reading path. */
internal data class SpineRef(
    val idref: String,
    val linear: Boolean,
)

/** What the package says about the book. [tocId] is the id of the older style contents file, if the spine names one. */
internal data class PackageDocument(
    val title: String?,
    val items: Map<String, ManifestItem>,
    val spine: List<SpineRef>,
    val tocId: String? = null,
)

/**
 * Reads the two small XML files that describe an EPUB: the container, which says where the package
 * is, and the package, which lists the book's files and their reading order. Android's pull parser
 * is used because it never fetches external entities. Prefixes on tag names are ignored, so
 * "opf:item" reads as "item". A damaged file makes the parser throw; the caller reports it.
 */
internal object EpubPackageParser {
    /** The path of the package file named by META-INF/container.xml, or null if it names none. */
    fun rootFilePath(container: InputStream): String? {
        val parser = newParser(container)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser.name) == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { resolve("", it) }
            }
            event = parser.next()
        }
        return null
    }

    fun parsePackage(
        input: InputStream,
        packagePath: String,
    ): PackageDocument {
        val parser = newParser(input)
        var title: String? = null
        var tocId: String? = null
        var inMetadata = false
        val items = LinkedHashMap<String, ManifestItem>()
        val spine = ArrayList<SpineRef>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (localName(parser.name)) {
                    "metadata" -> inMetadata = true
                    "title" -> if (inMetadata && title == null) title = parser.nextText().trim().takeIf { it.isNotEmpty() }
                    "item" -> readItem(parser, packagePath)?.let { items[it.id] = it }
                    "spine" -> tocId = parser.getAttributeValue(null, "toc")?.trim()?.takeIf { it.isNotEmpty() }
                    "itemref" -> readSpineRef(parser)?.let { spine.add(it) }
                }
            } else if (event == XmlPullParser.END_TAG && localName(parser.name) == "metadata") {
                inMetadata = false
            }
            event = parser.next()
        }
        return PackageDocument(title, items, spine, tocId)
    }

    /**
     * The path inside the EPUB that [href] points to, when [href] is written in the file at
     * [baseFile]: percent-escapes decoded, any #fragment or ?query dropped, "." and ".." resolved.
     * A path can never climb above the root of the book.
     */
    fun resolve(
        baseFile: String,
        href: String,
    ): String {
        val decoded = Uri.decode(href.substringBefore('#').substringBefore('?'))
        val baseDirectory = baseFile.substringBeforeLast('/', "")
        val joined =
            when {
                decoded.startsWith("/") -> decoded
                baseDirectory.isEmpty() -> decoded
                else -> "$baseDirectory/$decoded"
            }
        val parts = ArrayList<String>()
        for (part in joined.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts.add(part)
            }
        }
        return parts.joinToString("/")
    }

    /** The media type for a file name, for files the package does not list. */
    fun mimeTypeForName(path: String): String =
        when (path.substringAfterLast('.', "").lowercase()) {
            "xhtml", "xht" -> "application/xhtml+xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ncx" -> "application/x-dtbncx+xml"
            "xml" -> "application/xml"
            else -> "application/octet-stream"
        }

    private fun readItem(
        parser: XmlPullParser,
        packagePath: String,
    ): ManifestItem? {
        val id = parser.getAttributeValue(null, "id")?.trim()
        val href = parser.getAttributeValue(null, "href")?.trim()
        if (id.isNullOrEmpty() || href.isNullOrEmpty() || href.contains("://")) return null
        val path = resolve(packagePath, href)
        if (path.isEmpty()) return null
        return ManifestItem(
            id,
            path,
            parser.getAttributeValue(null, "media-type")?.trim(),
            parser.getAttributeValue(null, "properties")?.trim(),
        )
    }

    private fun readSpineRef(parser: XmlPullParser): SpineRef? {
        val idref = parser.getAttributeValue(null, "idref")?.trim()
        if (idref.isNullOrEmpty()) return null
        val linear = parser.getAttributeValue(null, "linear")?.trim()?.lowercase() != "no"
        return SpineRef(idref, linear)
    }

    internal fun localName(name: String?): String = name.orEmpty().substringAfter(':')

    internal fun newParser(input: InputStream): XmlPullParser {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)
        return parser
    }
}
