package com.freelibrary.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow

/**
 * The Sepia and Dim night reader pages must stay as readable as the app's own: the page's ink keeps 7 to 1, and so does every
 * reading text colour in the shade that suits the page (light shades on Sepia, dark shades on Dim night).
 */
class ReaderPageContrastTest {
    private val pages = readColors("src/main/res/values/colors_reader_pages.xml")
    private val lightShades = readColors("src/main/res/values/colors_reading.xml")
    private val darkShades = readColors("src/main/res/values-night/colors_reading.xml")

    @Test
    fun `Sepia ink and every light text colour keep 7 to 1 on the Sepia page`() {
        val page = pages.getValue("reader_page_sepia")
        assertAtLeast("sepia ink", pages.getValue("reader_page_sepia_ink"), page, READING_MINIMUM)
        lightShades.forEach { (name, colour) -> assertAtLeast("$name on sepia", colour, page, READING_MINIMUM) }
    }

    @Test
    fun `Dim night ink and every dark text colour keep 7 to 1 on the Dim night page`() {
        val page = pages.getValue("reader_page_dim")
        assertAtLeast("dim ink", pages.getValue("reader_page_dim_ink"), page, READING_MINIMUM)
        darkShades.forEach { (name, colour) -> assertAtLeast("$name on dim", colour, page, READING_MINIMUM) }
    }

    @Test
    fun `links and the search highlight stay readable on both pages`() {
        for (theme in listOf("sepia", "dim")) {
            val page = pages.getValue("reader_page_$theme")
            assertAtLeast("$theme link", pages.getValue("reader_page_${theme}_link"), page, TEXT_MINIMUM)
            assertAtLeast(
                "$theme ink on its highlight",
                pages.getValue("reader_page_${theme}_ink"),
                pages.getValue("reader_page_${theme}_highlight"),
                TEXT_MINIMUM,
            )
        }
    }

    private fun assertAtLeast(
        name: String,
        foreground: Int,
        background: Int,
        minimum: Double,
    ) {
        val ratio = contrast(foreground, background)
        assertTrue("$name is ${"%.2f".format(ratio)} to 1, needs $minimum", ratio >= minimum)
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

    private fun readColors(path: String): Map<String, Int> {
        val file = File(path)
        require(file.exists()) { "$path not found from ${File(".").absolutePath}" }
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("color")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val value = node.textContent.trim()
            require(Regex("#[0-9A-Fa-f]{6}").matches(value)) { "Unsupported color value $value" }
            node.attributes.getNamedItem("name").nodeValue to value.substring(1).toInt(16)
        }
    }

    private companion object {
        const val READING_MINIMUM = 7.0
        const val TEXT_MINIMUM = 4.5
    }
}
