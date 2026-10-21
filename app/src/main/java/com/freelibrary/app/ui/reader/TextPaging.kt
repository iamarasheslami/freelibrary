package com.freelibrary.app.ui.reader

/** The arithmetic of cutting laid out text into pages of whole lines. */
internal object TextPaging {
    /**
     * The first line of every page when [lineCount] lines are cut into pages [pageHeight] tall. A
     * page holds as many whole lines as fit, and at least one, so a tall line never stops the book.
     */
    fun pageStarts(
        lineCount: Int,
        lineTop: (Int) -> Int,
        lineBottom: (Int) -> Int,
        pageHeight: Int,
    ): IntArray {
        if (lineCount <= 0) return intArrayOf(0)
        val starts = ArrayList<Int>()
        var first = 0
        while (first < lineCount) {
            starts.add(first)
            val top = lineTop(first)
            var next = first + 1
            while (next < lineCount && lineBottom(next) - top <= pageHeight) next++
            first = next
        }
        return starts.toIntArray()
    }

    /** The page that [line] is on. */
    fun pageOfLine(
        starts: IntArray,
        line: Int,
    ): Int {
        var page = 0
        for (index in starts.indices) {
            if (starts[index] <= line) page = index else break
        }
        return page
    }

    /**
     * How far through a text of [length] characters [offset] is, in thousandths, rounded up so that
     * going back to that place never lands before [offset].
     */
    fun permilleOf(
        offset: Int,
        length: Int,
    ): Int {
        if (length <= 0) return 0
        val value = (offset.coerceIn(0, length).toLong() * MAX + length - 1) / length
        return value.toInt().coerceIn(0, MAX)
    }

    /** The character that [permille] thousandths through a text of [length] characters is at. */
    fun offsetOf(
        permille: Int,
        length: Int,
    ): Int = (length.toLong() * permille.coerceIn(0, MAX) / MAX).toInt().coerceIn(0, length)

    private const val MAX = 1000
}
