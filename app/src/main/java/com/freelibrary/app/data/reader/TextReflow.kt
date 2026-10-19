package com.freelibrary.app.data.reader

/**
 * Turns plain text into the paragraphs of a web page. Project Gutenberg texts end every line at
 * about 70 characters; in running prose those line ends are removed so the text fills the screen,
 * while verse, lists and indented text keep their line ends.
 */
internal object TextReflow {
    private val PARAGRAPH_BREAK = Regex("\\n[ \\t]*\\n(?:[ \\t]*\\n)*")

    /** Lines shorter than this, other than the last of a paragraph, mean the line ends are meant. */
    private const val MIN_WRAPPED_LINE = 35

    fun toHtml(text: String): String {
        val html = StringBuilder()
        for (paragraph in PARAGRAPH_BREAK.split(text)) {
            val lines = paragraph.trim('\n').split('\n').map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            if (isProse(lines)) {
                html.append("<p>").append(escape(lines.joinToString(" ") { it.trim() })).append("</p>\n")
            } else {
                html.append("<p style=\"white-space:pre-wrap\">").append(escape(lines.joinToString("\n"))).append("</p>\n")
            }
        }
        return html.toString()
    }

    private fun isProse(lines: List<String>): Boolean =
        lines.size == 1 ||
            (
                lines.none { it.startsWith("  ") || it.startsWith("\t") } &&
                    lines.dropLast(1).all { it.length >= MIN_WRAPPED_LINE }
            )

    private fun escape(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
