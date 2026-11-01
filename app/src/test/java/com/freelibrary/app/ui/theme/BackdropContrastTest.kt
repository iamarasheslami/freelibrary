package com.freelibrary.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.Inflater
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.math.pow

/**
 * The textures and washes behind the screens must never cost text its contrast. Each one is blended over the
 * page colour exactly as it is drawn, and the text colours that can sit on it are measured on the result.
 */
class BackdropContrastTest {
    private val light = readColors("src/main/res/values/colors.xml") + readColors("src/main/res/values/colors_backdrop.xml")
    private val dark = readColors("src/main/res/values-night/colors.xml") + readColors("src/main/res/values-night/colors_backdrop.xml")

    @Test
    fun `the grain is never more than 16 percent dark`() {
        val strongest = strongestAlpha(File("src/main/res/drawable-nodpi/bg_grain.png"))
        assertTrue("the strongest grain pixel has alpha $strongest", strongest in 1..MAX_GRAIN_ALPHA)
    }

    @Test
    fun `text on the grain keeps the AA contrast`() {
        val page = light.getValue("md_background")
        val darkest = blend(0x000000, MAX_GRAIN_ALPHA, page)
        for (text in listOf("md_on_surface", "md_on_surface_variant", "md_primary", "slider_heading")) {
            assertAtLeast(text, light.getValue(text), darkest)
        }
    }

    @Test
    fun `text on the light Home wash keeps the AA contrast`() = assertWash(light)

    @Test
    fun `text on the dark Home wash keeps the AA contrast`() = assertWash(dark)

    @Test
    fun `text on the middle of the dark vignette keeps the AA contrast`() {
        val centre = vignetteCentre()
        for (text in listOf("md_on_surface", "md_on_surface_variant", "md_primary", "slider_heading")) {
            assertAtLeast(text, dark.getValue(text), centre)
        }
    }

    private fun assertWash(colors: Map<String, Int>) {
        val page = colors.getValue("md_background")
        for (stop in listOf("backdrop_home_top", "backdrop_home_mid")) {
            val argb = colors.getValue(stop)
            val wash = blend(argb and RGB_MASK, argb ushr ALPHA_SHIFT, page)
            for (text in listOf("md_on_surface", "md_on_surface_variant", "md_primary", "slider_heading")) {
                assertAtLeast("$text on $stop", colors.getValue(text), wash)
            }
        }
    }

    private fun assertAtLeast(
        name: String,
        foreground: Int,
        background: Int,
    ) {
        val ratio = contrast(foreground, background)
        assertTrue("$name is ${"%.2f".format(ratio)} to 1, needs $TEXT_MINIMUM", ratio >= TEXT_MINIMUM)
    }

    /**
     * The largest alpha in an 8-bit RGBA PNG that is not interlaced. The JDK's image classes are not available to
     * Android unit tests, so the few PNG steps needed (chunks, inflate, undo the row filters) are done here.
     */
    private fun strongestAlpha(file: File): Int {
        val bytes = file.readBytes()
        var width = 0
        var height = 0
        val compressed = ByteArrayOutputStream()
        var position = PNG_SIGNATURE_SIZE
        while (position < bytes.size) {
            val length = ByteBuffer.wrap(bytes, position, 4).int
            val type = String(bytes, position + 4, 4, Charsets.US_ASCII)
            val body = position + CHUNK_HEADER_SIZE
            if (type == "IHDR") {
                val header = ByteBuffer.wrap(bytes, body, IHDR_SIZE)
                width = header.int
                height = header.int
                require(bytes[body + BIT_DEPTH_OFFSET].toInt() == 8 && bytes[body + COLOUR_TYPE_OFFSET].toInt() == RGBA) {
                    "the grain must be an 8-bit RGBA PNG"
                }
                require(bytes[body + INTERLACE_OFFSET].toInt() == 0) { "the grain must not be interlaced" }
            } else if (type == "IDAT") {
                compressed.write(bytes, body, length)
            }
            position = body + length + CHUNK_CRC_SIZE
        }
        val stride = width * BYTES_PER_PIXEL
        val raw = ByteArray(height * (stride + 1))
        val inflater = Inflater()
        inflater.setInput(compressed.toByteArray())
        var filled = 0
        while (filled < raw.size && !inflater.finished()) {
            val count = inflater.inflate(raw, filled, raw.size - filled)
            if (count == 0 && inflater.needsInput()) break
            filled += count
        }
        inflater.end()
        check(filled == raw.size) { "the grain image is incomplete" }

        var previous = IntArray(stride)
        var strongest = 0
        for (row in 0 until height) {
            val start = row * (stride + 1)
            val filter = raw[start].toInt()
            val current = IntArray(stride)
            for (index in 0 until stride) {
                val value = raw[start + 1 + index].toInt() and 0xFF
                val left = if (index >= BYTES_PER_PIXEL) current[index - BYTES_PER_PIXEL] else 0
                val up = previous[index]
                val upLeft = if (index >= BYTES_PER_PIXEL) previous[index - BYTES_PER_PIXEL] else 0
                val restored =
                    when (filter) {
                        0 -> value
                        1 -> value + left
                        2 -> value + up
                        3 -> value + (left + up) / 2
                        4 -> value + paeth(left, up, upLeft)
                        else -> error("unknown PNG filter $filter")
                    }
                current[index] = restored and 0xFF
            }
            for (pixel in 0 until width) strongest = maxOf(strongest, current[pixel * BYTES_PER_PIXEL + ALPHA_OFFSET])
            previous = current
        }
        return strongest
    }

    private fun paeth(
        left: Int,
        up: Int,
        upLeft: Int,
    ): Int {
        val estimate = left + up - upLeft
        val fromLeft = abs(estimate - left)
        val fromUp = abs(estimate - up)
        val fromUpLeft = abs(estimate - upLeft)
        return when {
            fromLeft <= fromUp && fromLeft <= fromUpLeft -> left
            fromUp <= fromUpLeft -> up
            else -> upLeft
        }
    }

    /** The middle of the dark vignette: its start colour, drawn over the dark page. */
    private fun vignetteCentre(): Int {
        val text = File("src/main/res/drawable-night/bg_window.xml").readText()
        val value = Regex("startColor=\"#([0-9A-Fa-f]{8})\"").find(text)?.groupValues?.get(1)
        requireNotNull(value) { "the vignette's start colour was not found" }
        val argb = value.toLong(16).toInt()
        return blend(argb and RGB_MASK, argb ushr ALPHA_SHIFT, dark.getValue("md_background"))
    }

    /** [overlay] at [alpha] (0 to 255) drawn over [page]; both are plain RGB. */
    private fun blend(
        overlay: Int,
        alpha: Int,
        page: Int,
    ): Int {
        val share = alpha / 255.0

        fun channel(shift: Int): Int {
            val top = (overlay shr shift) and 0xFF
            val bottom = (page shr shift) and 0xFF
            return Math.round(top * share + bottom * (1 - share)).toInt()
        }
        return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private fun contrast(
        first: Int,
        second: Int,
    ): Double {
        val (lighter, darker) = listOf(luminance(first), luminance(second)).sortedDescending()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((rgb shr shift) and 0xFF) / 255.0
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** Reads `#RRGGBB` and `#AARRGGBB` colours as ARGB ints; the first form is fully opaque. */
    private fun readColors(path: String): Map<String, Int> {
        val file = File(path)
        require(file.exists()) { "$path not found from ${File(".").absolutePath}" }
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("color")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val name = node.attributes.getNamedItem("name").nodeValue
            val value = node.textContent.trim()
            require(Regex("#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})").matches(value)) { "Unsupported color value $value" }
            val digits = value.substring(1)
            name to (if (digits.length == 6) "FF$digits" else digits).toLong(16).toInt()
        }
    }

    @Test
    fun `an 8 digit colour keeps its alpha apart from its colour`() {
        val top = light.getValue("backdrop_home_top")
        assertEquals(0x47, top ushr ALPHA_SHIFT)
        assertEquals(0x7AA05A, top and RGB_MASK)
    }

    private companion object {
        const val TEXT_MINIMUM = 4.5
        const val MAX_GRAIN_ALPHA = 40
        const val RGB_MASK = 0xFFFFFF
        const val ALPHA_SHIFT = 24
        const val PNG_SIGNATURE_SIZE = 8
        const val CHUNK_HEADER_SIZE = 8
        const val CHUNK_CRC_SIZE = 4
        const val IHDR_SIZE = 13
        const val BIT_DEPTH_OFFSET = 8
        const val COLOUR_TYPE_OFFSET = 9
        const val INTERLACE_OFFSET = 12
        const val RGBA = 6
        const val BYTES_PER_PIXEL = 4
        const val ALPHA_OFFSET = 3
    }
}
