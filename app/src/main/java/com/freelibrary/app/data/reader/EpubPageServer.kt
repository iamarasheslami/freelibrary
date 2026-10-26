package com.freelibrary.app.data.reader

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale
import kotlin.math.roundToInt

/**
 * How the reader paints a page: the colours of the app's current theme and the text size. With
 * [paged] the chapter is laid out in columns as wide as the screen, so the reader can show one
 * column at a time and turn between them; without it the chapter is one long page to scroll.
 */
data class ReaderStyle(
    val textColor: Int,
    val backgroundColor: Int,
    val linkColor: Int,
    val textPercent: Int = DEFAULT_TEXT_PERCENT,
    val paged: Boolean = false,
    val pageHeight: Int = 0,
    val rightToLeft: Boolean = false,
    /**
     * The app's size setting as a factor (0.85 for Small). The web view draws at the phone's own
     * density and ignores the app's, so the text size has to follow the setting through this.
     */
    val appScale: Float = 1f,
) {
    private val pageBoxHeight: String get() = if (pageHeight > 0) "${pageHeight}px" else "100vh"

    /** The style sheet laid over every page of the book, so the book follows the app's theme and size. */
    fun css(): String {
        val text = hex(textColor)
        val background = hex(backgroundColor)
        val link = hex(linkColor)
        val percent = (textPercent.coerceIn(MIN_TEXT_PERCENT, MAX_TEXT_PERCENT) * appScale).roundToInt()
        val sheet = if (paged) pagedCss(text, background, link, percent) else scrollCss(text, background, link, percent)
        return if (rightToLeft) sheet + "\n" + rightToLeftCss() else sheet
    }

    private fun scrollCss(
        text: String,
        background: String,
        link: String,
        percent: Int,
    ): String =
        """
        html, body { background-color: $background !important; color: $text !important; }
        body { font-size: $percent% !important; line-height: 1.6 !important; margin: 0 !important; padding: 16px 28px 32px 28px !important; overflow-wrap: break-word !important; }
        body * { color: inherit !important; }
        a, a * { color: $link !important; }
        img, svg, video { max-width: 100% !important; height: auto !important; }
        p, li, dd, blockquote { text-align: justify; }
        pre { white-space: pre-wrap !important; }
        """.trimIndent()

    private fun pagedCss(
        text: String,
        background: String,
        link: String,
        percent: Int,
    ): String =
        """
        html, body { background-color: $background !important; color: $text !important; margin: 0 !important; padding: 0 !important; }
        html { height: $pageBoxHeight !important; }
        body { box-sizing: border-box !important; height: $pageBoxHeight !important; padding: 24px 0 !important; column-width: 100vw !important; column-gap: 0 !important; column-fill: auto !important; font-size: $percent% !important; line-height: 1.6 !important; overflow-wrap: break-word !important; }
        body * { color: inherit !important; }
        a, a * { color: $link !important; }
        img, svg, video { max-width: 100% !important; max-height: 85vh !important; height: auto !important; }
        pre { white-space: pre-wrap !important; }
        h1, h2, h3 { break-after: avoid !important; }
        p { orphans: 2; widows: 2; }
        p, li, dd, blockquote { text-align: justify; }
        """.trimIndent()

    /**
     * In a right to left book the text runs right to left. In page view only the content does: the
     * columns keep their order, so the web view scrolls exactly as in any other book, and the reader
     * turns pages the other way round instead.
     */
    private fun rightToLeftCss(): String =
        if (paged) {
            "body > * { direction: rtl !important; }"
        } else {
            "body { direction: rtl !important; }"
        }

    private fun hex(color: Int): String = String.format(Locale.ROOT, "#%06X", color and 0xFFFFFF)

    companion object {
        const val DEFAULT_TEXT_PERCENT = 100
        const val MIN_TEXT_PERCENT = 70
        const val MAX_TEXT_PERCENT = 250
    }
}

/** One answer for the web view: what it is, how it is encoded (null for binary files) and its content. */
class ServedFile(
    val mimeType: String,
    val encoding: String?,
    val stream: InputStream,
)

/**
 * Answers the reader's web view from inside an open EPUB, so nothing is unpacked to disk and the
 * book never touches the network. Every page and file has an address on a made-up host that only
 * this server answers: [addressOf] makes the address of a path in the book, and [pathOf] reads it
 * back. Every chapter is served with the reader's style sheet linked in before its head ends; the
 * link carries a stamp of the style, so a changed style is never answered from the web view's cache.
 * A page larger than [maxPageBytes] is refused rather than held in memory.
 */
class EpubPageServer(
    private val book: EpubBook,
    private val style: ReaderStyle,
    private val maxPageBytes: Int = DEFAULT_MAX_PAGE_BYTES,
) {
    private val styleLink = """<link rel="stylesheet" type="text/css" href="$STYLE_URL?v=${style.css().hashCode()}"/>"""

    /** The address of [path] inside the book, optionally pointing at an element of the page. */
    fun addressOf(
        path: String,
        fragment: String? = null,
    ): String =
        Uri.Builder()
            .scheme("https")
            .authority(HOST)
            .encodedPath("/" + Uri.encode(path, "/"))
            .fragment(fragment)
            .build()
            .toString()

    /** The path inside the book an address points at, or null if it is not one of this server's addresses. */
    fun pathOf(url: String): String? {
        val uri = Uri.parse(url)
        if (!uri.scheme.equals("https", ignoreCase = true) || !uri.host.equals(HOST, ignoreCase = true)) return null
        return uri.encodedPath
            ?.let { EpubPackageParser.resolve("", it) }
            ?.takeIf { it.isNotEmpty() }
    }

    /** The file at [path] in the book (or the style sheet), or null if there is nothing to serve there. */
    fun serve(path: String): ServedFile? {
        if (path == STYLE_PATH) return ServedFile("text/css", "utf-8", ByteArrayInputStream(style.css().toByteArray()))
        val mimeType = book.mimeType(path)
        if (mimeType == XHTML || mimeType == HTML) return servePage(path, mimeType)
        val stream = book.read(path) ?: return null
        return ServedFile(mimeType, null, stream)
    }

    private fun servePage(
        path: String,
        mimeType: String,
    ): ServedFile? {
        val bytes = book.read(path)?.use { readAtMost(it, maxPageBytes) } ?: return null
        val page = String(bytes, Charsets.UTF_8)
        val headEnd = HEAD_END.find(page)?.range?.first
        val styled =
            if (headEnd == null) {
                page
            } else {
                page.substring(0, headEnd) + styleLink + page.substring(headEnd)
            }
        return ServedFile(mimeType, "utf-8", ByteArrayInputStream(styled.toByteArray()))
    }

    private fun readAtMost(
        input: InputStream,
        max: Int,
    ): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > max) return null
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    companion object {
        /** A host that cannot exist, so a request to it can only ever be answered by this server. */
        const val HOST = "epub.invalid"
        const val STYLE_PATH = "__freelibrary__/reader.css"
        const val STYLE_URL = "https://$HOST/$STYLE_PATH"
        const val DEFAULT_MAX_PAGE_BYTES = 8 * 1024 * 1024

        private const val BUFFER_BYTES = 8192
        private const val XHTML = "application/xhtml+xml"
        private const val HTML = "text/html"
        private val HEAD_END = Regex("</head\\s*>", RegexOption.IGNORE_CASE)
    }
}
