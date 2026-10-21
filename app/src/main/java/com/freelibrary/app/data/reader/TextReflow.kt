package com.freelibrary.app.data.reader

/**
 * Turns plain text into paragraphs for the screen. Project Gutenberg texts end every line at
 * about 70 characters; in running prose those line ends are removed so the text fills the screen,
 * while verse, lists and indented text keep their line ends.
 */
internal object TextReflow {
    /** Lines shorter than this, other than the last of a paragraph, mean the line ends are meant. */
    private const val MIN_WRAPPED_LINE = 35

    /** The paragraphs of [text], one blank line apart. A paragraph is a run of lines that are not blank. */
    fun toPlain(text: String): String {
        val out = StringBuilder()
        val lines = ArrayList<String>()

        fun finishParagraph() {
            if (lines.isEmpty()) return
            if (out.isNotEmpty()) out.append("\n\n")
            if (isProse(lines)) {
                out.append(lines.joinToString(" ") { it.trim() })
            } else {
                out.append(lines.joinToString("\n"))
            }
            lines.clear()
        }

        for (raw in text.split('\n')) {
            val line = raw.trimEnd()
            if (line.isBlank()) finishParagraph() else lines.add(line)
        }
        finishParagraph()
        return out.toString()
    }

    private fun isProse(lines: List<String>): Boolean =
        lines.size == 1 ||
            (
                lines.none { it.startsWith("  ") || it.startsWith("\t") } &&
                    lines.dropLast(1).all { it.length >= MIN_WRAPPED_LINE }
            )
}
