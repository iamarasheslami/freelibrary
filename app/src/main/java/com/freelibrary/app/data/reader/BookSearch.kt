package com.freelibrary.app.data.reader

/**
 * One place where a search found its words: the [unit] (chapter or part) it is in, its [offset] in that unit's
 * text, which [occurrence] of the words in that unit it is (from 0), and a little text [before] and [after] it
 * for the results list. [length] is the length of the words as found.
 */
data class SearchHit(
    val unit: Int,
    val offset: Int,
    val occurrence: Int,
    val length: Int,
    val before: String,
    val match: String,
    val after: String,
)

/** What a search found: [hits] in book order, and whether more were left out because of [BookSearch.MAX_HITS]. */
data class SearchResults(
    val hits: List<SearchHit>,
    val capped: Boolean,
)

/**
 * Searching the text of a book, ignoring upper and lower case. It uses plain string search, never a regular
 * expression: regular expressions over a whole book crashed the app's first text reader on old phones.
 */
object BookSearch {
    /** The fewest characters a search is run for; one letter would match nearly everywhere. */
    const val MIN_QUERY_CHARS = 2

    /** The most places a search lists. */
    const val MAX_HITS = 200

    private const val CONTEXT_CHARS = 40

    /** [query] without surrounding spaces and with every run of white space turned into one space. */
    fun clean(query: String): String {
        val out = StringBuilder(query.length)
        var space = false
        for (c in query.trim()) {
            if (c.isWhitespace()) {
                space = true
            } else {
                if (space && out.isNotEmpty()) out.append(' ')
                space = false
                out.append(c)
            }
        }
        return out.toString()
    }

    /** The places of [query] in [text], a unit numbered [unit], at most [limit] of them. */
    fun find(
        text: String,
        query: String,
        unit: Int,
        limit: Int = MAX_HITS,
    ): List<SearchHit> {
        if (query.isEmpty() || limit <= 0) return emptyList()
        val hits = ArrayList<SearchHit>()
        var from = 0
        var occurrence = 0
        while (hits.size < limit) {
            val at = text.indexOf(query, from, ignoreCase = true)
            if (at < 0) break
            val end = at + query.length
            hits.add(
                SearchHit(
                    unit = unit,
                    offset = at,
                    occurrence = occurrence,
                    length = query.length,
                    before = context(text, maxOf(0, at - CONTEXT_CHARS), at, ellipsisAtStart = at - CONTEXT_CHARS > 0),
                    match = text.substring(at, end).let { flatten(it) },
                    after = context(text, end, minOf(text.length, end + CONTEXT_CHARS), ellipsisAtEnd = end + CONTEXT_CHARS < text.length),
                ),
            )
            occurrence++
            from = end
        }
        return hits
    }

    /**
     * Searches the units 0 until [unitCount], whose text [textOf] gives (null for a unit with none), and stops
     * once [MAX_HITS] places are found. [checkActive] is called between units, so a search that is no longer
     * wanted can stop by throwing.
     */
    fun findAll(
        unitCount: Int,
        query: String,
        checkActive: () -> Unit = {},
        textOf: (Int) -> String?,
    ): SearchResults {
        val hits = ArrayList<SearchHit>()
        var capped = false
        for (unit in 0 until unitCount) {
            checkActive()
            val text = textOf(unit) ?: continue
            val room = MAX_HITS - hits.size
            val found = find(text, query, unit, room + 1)
            if (found.size > room) {
                hits.addAll(found.take(room))
                capped = true
                break
            }
            hits.addAll(found)
        }
        return SearchResults(hits, capped)
    }

    private fun context(
        text: String,
        start: Int,
        end: Int,
        ellipsisAtStart: Boolean = false,
        ellipsisAtEnd: Boolean = false,
    ): String {
        val flat = flatten(text.substring(start, end))
        return (if (ellipsisAtStart) "\u2026" else "") + flat + (if (ellipsisAtEnd) "\u2026" else "")
    }

    /** [text] with every run of white space, such as a line end, turned into one space. */
    private fun flatten(text: String): String {
        val out = StringBuilder(text.length)
        var space = false
        for (c in text) {
            if (c.isWhitespace()) {
                space = true
            } else {
                if (space) out.append(' ')
                space = false
                out.append(c)
            }
        }
        if (space) out.append(' ')
        return out.toString()
    }
}
