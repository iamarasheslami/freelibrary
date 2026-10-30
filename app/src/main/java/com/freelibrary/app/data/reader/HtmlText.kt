package com.freelibrary.app.data.reader

/**
 * The words of an XHTML page, for searching a chapter: tags removed, the contents of script and style
 * elements dropped, and the usual character references turned into characters. A tag that ends a line or
 * a paragraph becomes a space, any other tag (bold, a link) becomes nothing, so a word split by markup
 * stays one word. It reads the page with plain loops, never a regular expression.
 */
object HtmlText {
    private val BLOCK_TAGS =
        setOf(
            "p", "div", "br", "li", "ul", "ol", "tr", "td", "th", "table", "section", "article", "blockquote",
            "h1", "h2", "h3", "h4", "h5", "h6", "hr", "pre", "dd", "dt", "figure", "figcaption", "header", "footer", "aside",
        )
    private val HIDDEN_TAGS = setOf("script", "style", "head")
    private val NAMED =
        mapOf(
            "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
            "mdash" to "\u2014", "ndash" to "\u2013", "hellip" to "\u2026", "lsquo" to "\u2018", "rsquo" to "\u2019",
            "ldquo" to "\u201C", "rdquo" to "\u201D",
        )
    private const val LONGEST_REFERENCE = 10

    fun toText(html: String): String {
        val out = StringBuilder(html.length / 2)
        var hiddenUntil: String? = null
        var i = 0
        while (i < html.length) {
            val c = html[i]
            if (c == '<') {
                val close = html.indexOf('>', i)
                if (close < 0) break
                val name = tagName(html, i + 1, close)
                val closing = html.startsWith("</", i)
                if (hiddenUntil != null) {
                    if (closing && name == hiddenUntil) hiddenUntil = null
                } else if (!closing && name in HIDDEN_TAGS && !html.startsWith("/>", close - 1)) {
                    hiddenUntil = name
                } else if (name in BLOCK_TAGS) {
                    out.append(' ')
                }
                i = close + 1
            } else if (hiddenUntil != null) {
                i++
            } else if (c == '&') {
                val end = html.indexOf(';', i)
                val decoded = if (end in i + 2..i + LONGEST_REFERENCE) decode(html.substring(i + 1, end)) else null
                if (decoded != null) {
                    out.append(decoded)
                    i = end + 1
                } else {
                    out.append(c)
                    i++
                }
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun tagName(
        html: String,
        from: Int,
        close: Int,
    ): String {
        var start = from
        if (start < close && html[start] == '/') start++
        var end = start
        while (end < close && !html[end].isWhitespace() && html[end] != '/') end++
        return html.substring(start, end).lowercase()
    }

    private fun decode(reference: String): String? {
        if (reference.startsWith("#")) {
            val number =
                if (reference.startsWith("#x") || reference.startsWith("#X")) {
                    reference.substring(2).toIntOrNull(HEX)
                } else {
                    reference.substring(1).toIntOrNull()
                }
            return number?.takeIf { it in 1..Character.MAX_CODE_POINT }?.let { String(Character.toChars(it)) }
        }
        return NAMED[reference]
    }

    private const val HEX = 16
}
