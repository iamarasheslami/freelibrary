package com.freelibrary.app.data.reader

import org.xmlpull.v1.XmlPullParser

/** One line of a book's contents as written in the book: [path] is the file inside the EPUB it points to. */
internal data class RawContentsEntry(
    val title: String,
    val path: String,
    val level: Int,
)

/**
 * Reads a book's table of contents from the file the book keeps it in: the navigation document of
 * a newer EPUB, or the NCX file of an older one. Nothing is fetched from outside the book. A
 * damaged file makes the parser throw; the caller treats that as a book without contents.
 */
internal object EpubContentsParser {
    /** The first "toc" navigation of the navigation document [navPath], with how deeply each entry is nested. */
    fun parseNav(
        input: java.io.InputStream,
        navPath: String,
    ): List<RawContentsEntry> {
        val parser = EpubPackageParser.newParser(input)
        val entries = ArrayList<RawContentsEntry>()
        var inToc = false
        var listDepth = 0
        var href: String? = null
        val title = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG ->
                    when (EpubPackageParser.localName(parser.name)) {
                        "nav" -> if (!inToc && isContentsNav(parser)) inToc = true
                        "ol", "ul" -> if (inToc) listDepth++
                        "a" ->
                            if (inToc) {
                                href = parser.getAttributeValue(null, "href")?.trim()
                                title.setLength(0)
                            }
                    }
                XmlPullParser.TEXT -> if (inToc && href != null) title.append(parser.text)
                XmlPullParser.END_TAG ->
                    when (EpubPackageParser.localName(parser.name)) {
                        "nav" -> if (inToc) return entries
                        "ol", "ul" -> if (inToc) listDepth--
                        "a" ->
                            if (inToc && href != null) {
                                addEntry(entries, title.toString(), href, navPath, listDepth - 1)
                                href = null
                            }
                    }
            }
            event = parser.next()
        }
        return entries
    }

    /** The entries of the NCX file [ncxPath], with how deeply each one is nested. */
    fun parseNcx(
        input: java.io.InputStream,
        ncxPath: String,
    ): List<RawContentsEntry> {
        val parser = EpubPackageParser.newParser(input)
        val entries = ArrayList<RawContentsEntry>()
        val labels = ArrayList<String>()
        var inLabel = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (EpubPackageParser.localName(parser.name)) {
                    "navPoint" -> labels.add("")
                    "navLabel" -> inLabel = true
                    "text" -> if (inLabel && labels.isNotEmpty()) labels[labels.lastIndex] = parser.nextText()
                    "content" ->
                        if (labels.isNotEmpty()) {
                            addEntry(entries, labels.last(), parser.getAttributeValue(null, "src")?.trim(), ncxPath, labels.size - 1)
                        }
                }
            } else if (event == XmlPullParser.END_TAG) {
                when (EpubPackageParser.localName(parser.name)) {
                    "navLabel" -> inLabel = false
                    "navPoint" -> if (labels.isNotEmpty()) labels.removeAt(labels.lastIndex)
                }
            }
            event = parser.next()
        }
        return entries
    }

    private fun isContentsNav(parser: XmlPullParser): Boolean {
        for (i in 0 until parser.attributeCount) {
            if (EpubPackageParser.localName(parser.getAttributeName(i)) == "type" &&
                parser.getAttributeValue(i).trim().split(Regex("\\s+")).contains("toc")
            ) {
                return true
            }
        }
        return false
    }

    private fun addEntry(
        entries: MutableList<RawContentsEntry>,
        rawTitle: String,
        href: String?,
        baseFile: String,
        level: Int,
    ) {
        if (href.isNullOrEmpty() || href.startsWith("#") || href.contains("://")) return
        val path = EpubPackageParser.resolve(baseFile, href)
        if (path.isEmpty()) return
        val title = rawTitle.replace(Regex("\\s+"), " ").trim().take(MAX_TITLE_LENGTH)
        entries.add(RawContentsEntry(title, path, level.coerceAtLeast(0)))
    }

    private const val MAX_TITLE_LENGTH = 200
}
