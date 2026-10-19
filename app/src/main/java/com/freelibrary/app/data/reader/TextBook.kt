package com.freelibrary.app.data.reader

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Why a file could not be opened as a plain text book. */
enum class TextProblem {
    /** The file could not be read. */
    UNREADABLE,

    /** The file has no text in it. */
    EMPTY,

    /** The file is too large to hold in memory on an old phone. */
    TOO_LARGE,
}

sealed interface TextOpenResult {
    data class Opened(
        val book: TextBook,
    ) : TextOpenResult

    data class Failed(
        val problem: TextProblem,
    ) : TextOpenResult
}

/** One part of a plain text book: the characters from [start] up to [end] of its text. */
data class TextPart(
    val start: Int,
    val end: Int,
)

/**
 * A plain text book, read whole and cut into [parts] of about [PART_TARGET_CHARS] characters at
 * paragraph breaks, so a long book is shown a part at a time. A place in the book is the number of
 * characters before it, counted in the text as the reader holds it (line ends as "\n", no byte
 * order mark); [locate] and [offsetOf] convert between such a number and a part and how far through it.
 */
class TextBook private constructor(
    private val text: String,
    val parts: List<TextPart>,
) {
    /** The number of characters in the book. */
    val length: Int get() = text.length

    /** The page for part [part], painted with [style]. */
    fun html(
        part: Int,
        style: ReaderStyle,
    ): String {
        val range = parts[part]
        val body = TextReflow.toHtml(text.substring(range.start, range.end))
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/><title></title>" +
            "<style>${style.css()}</style></head><body>$body</body></html>"
    }

    /** The place [permille] thousandths of the way through part [part], as a number of characters. */
    fun offsetOf(
        part: Int,
        permille: Int,
    ): Int {
        val range = parts[part.coerceIn(0, parts.lastIndex)]
        val inside = (range.end - range.start).toLong() * permille.coerceIn(0, MAX_PERMILLE) / MAX_PERMILLE
        return (range.start + inside).toInt().coerceIn(0, length)
    }

    /** The part the place [offset] is in, and how far through that part it is in thousandths. */
    fun locate(offset: Int): Pair<Int, Int> {
        val place = offset.coerceIn(0, length)
        val part = parts.indexOfLast { it.start <= place }.coerceAtLeast(0)
        val range = parts[part]
        val permille = ((place - range.start).toLong() * MAX_PERMILLE / (range.end - range.start)).toInt()
        return part to permille.coerceIn(0, MAX_PERMILLE)
    }

    companion object {
        /** Files larger than this are not opened: the text is held in memory, twice as large as the file. */
        const val MAX_FILE_BYTES = 12L * 1024 * 1024

        /** About how many characters a part has; it ends at the next paragraph break after this many. */
        const val PART_TARGET_CHARS = 40_000

        /** A last part shorter than this is joined to the one before it. */
        private const val MIN_LAST_PART_CHARS = 2_000

        private const val MAX_PERMILLE = 1000
        private val LINE_BREAKS = Regex("\\n(?:[ \\t]*\\n)*")

        /** Opens [file]. Reads files from disk, so call it off the main thread. */
        fun open(file: File): TextOpenResult {
            if (!file.isFile) return TextOpenResult.Failed(TextProblem.UNREADABLE)
            if (file.length() > MAX_FILE_BYTES) return TextOpenResult.Failed(TextProblem.TOO_LARGE)
            val bytes =
                try {
                    file.readBytes()
                } catch (e: IOException) {
                    return TextOpenResult.Failed(TextProblem.UNREADABLE)
                } catch (e: OutOfMemoryError) {
                    return TextOpenResult.Failed(TextProblem.TOO_LARGE)
                }
            val book = fromText(decode(bytes)) ?: return TextOpenResult.Failed(TextProblem.EMPTY)
            return TextOpenResult.Opened(book)
        }

        /** The book for [raw] text, or null when it has nothing to read. */
        fun fromText(raw: String): TextBook? {
            val text = raw.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
            if (text.isBlank()) return null
            return TextBook(text, split(text))
        }

        /** Text as UTF-8 if it is valid UTF-8, otherwise as Windows-1252, which older files are written in. */
        private fun decode(bytes: ByteArray): String {
            val decoder =
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
            return try {
                decoder.decode(ByteBuffer.wrap(bytes)).toString()
            } catch (e: CharacterCodingException) {
                String(bytes, Charset.forName("windows-1252"))
            }
        }

        private fun split(text: String): List<TextPart> {
            val parts = ArrayList<TextPart>()
            var start = 0
            for (match in LINE_BREAKS.findAll(text)) {
                val end = match.range.last + 1
                val size = end - start
                val paragraphBreak = match.value.count { it == '\n' } >= 2
                // Cut at a paragraph break; with none in sight, at any line end once the part is twice as long.
                if (size >= PART_TARGET_CHARS && (paragraphBreak || size >= 2 * PART_TARGET_CHARS)) {
                    parts.add(TextPart(start, end))
                    start = end
                }
            }
            if (start < text.length) {
                if (parts.isNotEmpty() && text.length - start < MIN_LAST_PART_CHARS) {
                    parts[parts.lastIndex] = TextPart(parts.last().start, text.length)
                } else {
                    parts.add(TextPart(start, text.length))
                }
            }
            return parts
        }
    }
}
